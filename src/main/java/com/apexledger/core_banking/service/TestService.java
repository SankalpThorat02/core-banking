package com.apexledger.core_banking.service;

import com.apexledger.core_banking.dto.CreateTestRequest;
import com.apexledger.core_banking.dto.TestSummary;
import com.apexledger.core_banking.entity.MediaMaster;
import com.apexledger.core_banking.entity.RecruitTest;
import com.apexledger.core_banking.entity.RecruitTestQuestion;
import com.apexledger.core_banking.repository.MediaMasterRepository;
import com.apexledger.core_banking.repository.RecruitTestQuestionRepository;
import com.apexledger.core_banking.repository.RecruitTestRepository;
import com.apexledger.core_banking.util.ApiResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestService {

    @Autowired
    private MediaMasterRepository mediaMasterRepository;

    @Autowired
    private RecruitTestRepository recruitTestRepository;

    @Autowired
    private RecruitTestQuestionRepository recruitTestQuestionRepository;

    @Autowired
    private AssessmentQueryService assessmentQueryService;

    private static final String[] OBJECTIVE_HEADERS = {
            "QUESTION_LEVEL", "QUESTION", "OPTION_A", "OPTION_B", "OPTION_C", "OPTION_D", "CORRECT_ANSWER", "QUESTION_TYPE", "MARKS"
    };

    private static final String[] SUBJECTIVE_HEADERS = {
            "QUESTION_LEVEL", "QUESTION", "MARKS"
    };

    public ApiResponse<?> getAllTestSummaries() {
        try {
            List<TestSummary> summaries = recruitTestRepository.findAllTestSummaries();
            return new ApiResponse<>("SUCCESS", "Fetched " + summaries.size() + " tests", summaries);
        } catch (Exception e) {
            return new ApiResponse<>("ERROR", "Failed to fetch tests: " + e.getMessage(), null);
        }
    }

    public ApiResponse<?> getTestDetails(Long testId) {
        try {
            Optional<RecruitTest> testOpt = recruitTestRepository.findByIdWithQuestions(testId);

            if (testOpt.isEmpty()) {
                return new ApiResponse<>("ERROR", "Test not found with ID: " + testId, null);
            }

            return new ApiResponse<>("SUCCESS", "Test details fetched successfully", testOpt.get());
        } catch (Exception e) {
            return new ApiResponse<>("ERROR", "Failed to fetch test details: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<?> createTest(CreateTestRequest request, MultipartFile objectiveFile, MultipartFile subjectiveFile) {
        try {
            // 1. Ensure at least one file is uploaded
            boolean hasObjective = objectiveFile != null && !objectiveFile.isEmpty();
            boolean hasSubjective = subjectiveFile != null && !subjectiveFile.isEmpty();

            if (!hasObjective && !hasSubjective) {
                return new ApiResponse<>("ERROR", "Please upload at least one test file (Objective or Subjective).", null);
            }

            // 2. Resolve Test Name based on whichever file is present
            String originalFileName = hasObjective ? objectiveFile.getOriginalFilename() : subjectiveFile.getOriginalFilename();
            String finalTestName = resolveTestName(request.getTestName(), originalFileName);


            // Master list to hold all questions before saving to DB
            List<RecruitTestQuestion> allQuestions = new ArrayList<>();
            int objCount = 0;
            int subCount = 0;

            // 4. Process Objective File (If Present)
            if (hasObjective) {
                validateUploadFile(objectiveFile);

                ParsedData objData = parseObjectiveExcelFile(objectiveFile, null);

                persistImagesAndResolveTokens(objData.questions(), objData.images());
                allQuestions.addAll(objData.questions());

                objCount = objData.questions().size();
            }

            // 5. Process Subjective File (If Present)
            if (hasSubjective) {
                validateUploadFile(subjectiveFile);

                ParsedData subData = parseSubjectiveExcelFile(subjectiveFile, null);

                persistImagesAndResolveTokens(subData.questions(), subData.images());
                allQuestions.addAll(subData.questions());

                subCount = subData.questions().size();
            }

            int totalAppeared = objCount + subCount;

            Long TestId = assessmentQueryService.saveTestDetailsProcedure(
                    request, finalTestName, totalAppeared, objCount, subCount
            );

            for (RecruitTestQuestion question : allQuestions) {
                assessmentQueryService.saveQuestionProcedure(TestId, question);
            }

            return new ApiResponse<>("SUCCESS", "Test parsed Successfully with " + allQuestions.size() + " total questions", null);

        } catch (ExcelValidationException ex) {
            return new ApiResponse<>("ERROR", "Excel Validation Error", ex.getErrors());
        } catch (Exception ex) {
            return new ApiResponse<>("ERROR", "An unexpected error occurred: " + ex.getMessage(), null);
        }
    }

    private void validateHeaders(Row headerRow, String[] expectedHeaders) {
        if (headerRow == null) {
            throw new ExcelValidationException(List.of("Header row is missing."));
        }

        List<String> errors = new ArrayList<>();
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null || cell.getCellType() != CellType.STRING ||
                    !expectedHeaders[i].equalsIgnoreCase(cell.getStringCellValue().trim())) {
                errors.add("Invalid header at column " + (i+1) + ". Expected: '" + expectedHeaders[i] + "'");
            }
        }

        if (!errors.isEmpty()) {
            throw new ExcelValidationException(errors);
        }
    }

    // ==========================================
    // OBJECTIVE PARSER
    // ==========================================
    public ParsedData parseObjectiveExcelFile(MultipartFile file, RecruitTest test) throws IOException {
        List<String> errors = new ArrayList<>();
        List<RecruitTestQuestion> questions = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            if (sheet == null || sheet.getLastRowNum() <= 0) {
                throw new ExcelValidationException(List.of("Objective Excel file does not contain any question rows."));
            }

            validateHeaders(sheet.getRow(0), OBJECTIVE_HEADERS);

            Map<CellAddress, ExtractedImage> imagesByCell = extractImages(sheet, errors);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (isRowEmpty(row, formatter, 8) && !imagesByCell.containsKey(new CellAddress(rowIndex, 1))) {
                    continue;
                }

                RecruitTestQuestion question = mapObjectiveQuestion(row, rowIndex, formatter, imagesByCell, errors);
                if (question != null) {
                    question.setTest(test); // Link to parent
                    questions.add(question);
                }
            }

            if (!errors.isEmpty()) throw new ExcelValidationException(errors);

            System.out.println("Successfully extracted " + questions.size() + " objective questions.");
            return new ParsedData(questions, imagesByCell);
        }
    }

    private RecruitTestQuestion mapObjectiveQuestion(Row row, int rowIndex, DataFormatter formatter, Map<CellAddress, ExtractedImage> imagesByCell, List<String> errors) {
        int displayRow = rowIndex + 1;

        String levelStr = getCellValue(row, 0, formatter);
        String questionLevel = levelStr.isEmpty() ? "M" : levelStr.substring(0, 1).toUpperCase(Locale.ROOT);

        String questionText = getCellValue(row, 1, formatter);
        String optionA = getCellValue(row, 2, formatter);
        String optionB = getCellValue(row, 3, formatter);
        String optionC = getCellValue(row, 4, formatter);
        String optionD = getCellValue(row, 5, formatter);
        String correctAnswer = getCellValue(row, 6, formatter).replace(" ", "").toUpperCase(Locale.ROOT);
        String questionType = getCellValue(row, 7, formatter).toLowerCase(Locale.ROOT);
        String marksStr = getCellValue(row, 8, formatter);

        questionText = appendImageToken(questionText, rowIndex, 1, imagesByCell);
        optionA = appendImageToken(optionA, rowIndex, 2, imagesByCell);
        optionB = appendImageToken(optionB, rowIndex, 3, imagesByCell);
        optionC = appendImageToken(optionC, rowIndex, 4, imagesByCell);
        optionD = appendImageToken(optionD, rowIndex, 5, imagesByCell);

        if (!correctAnswer.matches("^[A-D](,[A-D])*$")) {
            errors.add("Row " + displayRow + ": Correct Answer must be A, B, C, or D (comma separated for multiple).");
        }

        Double marks = 0.0;
        try {
            marks = Double.parseDouble(marksStr);
            if (marks <= 0 ) {
                errors.add("Row " + displayRow + ": Marks must be greater than 0.");
            }
        } catch (NumberFormatException e) {
            errors.add("Row " + displayRow + ": Marks must be a number.");
            return null;
        }

        if (questionType.equals("boolean")) {
            if (!optionC.isEmpty() || !optionD.isEmpty()) {
                errors.add("Row " + displayRow + ": Boolean questions cannot have Option C or D.");
            }
        }

        return RecruitTestQuestion.builder()
                .questionLevel(questionLevel)
                .question(questionText)
                .optionA(optionA)
                .optionB(optionB)
                .optionC(optionC)
                .optionD(optionD)
                .correctAnswer(correctAnswer)
                .questionType(questionType)
                .marks(marks)
                .build();
    }

    // ==========================================
    // SUBJECTIVE PARSER
    // ==========================================
    public ParsedData parseSubjectiveExcelFile(MultipartFile file, RecruitTest test) throws IOException {
        List<String> errors = new ArrayList<>();
        List<RecruitTestQuestion> questions = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            if (sheet == null || sheet.getLastRowNum() <= 0) {
                throw new ExcelValidationException(List.of("Subjective Excel file does not contain any question rows."));
            }

            validateHeaders(sheet.getRow(0), SUBJECTIVE_HEADERS);

            Map<CellAddress, ExtractedImage> imagesByCell = extractImages(sheet, errors);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                // Assuming Subjective only uses up to column 2 (Question, Marks)
                if (isRowEmpty(row, formatter, 2) && !imagesByCell.containsKey(new CellAddress(rowIndex, 1))) {
                    continue;
                }

                RecruitTestQuestion question = mapSubjectiveQuestion(row, rowIndex, formatter, imagesByCell, errors);
                if (question != null) {
                    question.setTest(test); // Link to parent
                    questions.add(question);
                }
            }

            if (!errors.isEmpty()) throw new ExcelValidationException(errors);

            System.out.println("Successfully extracted " + questions.size() + " subjective questions.");
            return new ParsedData(questions, imagesByCell);
        }
    }

    private RecruitTestQuestion mapSubjectiveQuestion(Row row, int rowIndex, DataFormatter formatter, Map<CellAddress, ExtractedImage> imagesByCell, List<String> errors) {
        int displayRow = rowIndex + 1;

        String levelStr = getCellValue(row, 0, formatter);
        String questionLevel = levelStr.isEmpty() ? "M" : levelStr.substring(0, 1).toUpperCase(Locale.ROOT);

        // Assuming Column 1 is Question, Column 2 is Marks for Subjective
        String questionText = getCellValue(row, 1, formatter);
        String marksStr = getCellValue(row, 2, formatter);

        questionText = appendImageToken(questionText, rowIndex, 1, imagesByCell);

        Double marks = 0.0;
        try {
            if (!marksStr.isEmpty()) {
                marks = Double.parseDouble(marksStr);
            }
        } catch (NumberFormatException e) {
            errors.add("Row " + displayRow + ": Marks must be a number.");
            return null;
        }

        return RecruitTestQuestion.builder()
                .questionLevel(questionLevel)
                .question(questionText)
                .optionA(null)        // Forced to Null
                .optionB(null)        // Forced to Null
                .optionC(null)        // Forced to Null
                .optionD(null)        // Forced to Null
                .correctAnswer(null)  // Forced to Null
                .questionType("subjective")
                .marks(marks)
                .build();
    }

    // ==========================================
    // UTILITIES
    // ==========================================
    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null) return "";

        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";

        // Safely evaluate formulas
        if (cell.getCellType() == CellType.FORMULA) {
            FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
            CellValue cellValue = evaluator.evaluate(cell);
            if (cellValue == null) return "";
            return cellValue.formatAsString();
        }

        return formatter.formatCellValue(cell).trim();
    }

    private String appendImageToken(String text, int row, int col, Map<CellAddress, ExtractedImage> imagesByCell) {
        if (!imagesByCell.containsKey(new CellAddress(row, col))) {
            return text;
        }

        String token = String.format("[[IMG_TMP:%d:%d]]", row, col);
        if (text == null || text.trim().isEmpty()) {
            return token;
        }

        return token + " \n " + text;
    }

    // Updated to accept how many columns to check so it works for both Obj and Sub files
    private boolean isRowEmpty(Row row, DataFormatter formatter, int columnsToCheck) {
        if (row == null) return true;
        for (int i = 0; i <= columnsToCheck; i++) {
            if (!getCellValue(row, i, formatter).isEmpty()) return false;
        }
        return true;
    }

    private int persistImagesAndResolveTokens(List<RecruitTestQuestion> questions, Map<CellAddress, ExtractedImage> imagesByCell) {
        Map<CellAddress, String> realTokens = new HashMap<>();

        for (Map.Entry<CellAddress, ExtractedImage> entry : imagesByCell.entrySet()) {
            CellAddress address = entry.getKey();
            ExtractedImage image = entry.getValue();

            String generatedFileName = String.format("excel-img-r%d-c%d.%s", (address.row() + 1), (address.col() + 1), image.extension());

            MediaMaster media = MediaMaster.builder()
                    .fileName(generatedFileName)
                    .contentType(image.contentType())
                    .fileSize((long) image.data().length)
                    .fileData(image.data())
                    .createdAt(LocalDateTime.now())
                    .build();

            media = mediaMasterRepository.save(media);
            realTokens.put(address, "[[IMG:" + media.getId() + "]]");
        }

        for (RecruitTestQuestion question : questions) {
            question.setQuestion(swapTempTokens(question.getQuestion(), realTokens));
            question.setOptionA(swapTempTokens(question.getOptionA(), realTokens));
            question.setOptionB(swapTempTokens(question.getOptionB(), realTokens));
            question.setOptionC(swapTempTokens(question.getOptionC(), realTokens));
            question.setOptionD(swapTempTokens(question.getOptionD(), realTokens));
        }

        return realTokens.size();
    }

    private String swapTempTokens(String text, Map<CellAddress, String> realTokens) {
        if (text == null || text.trim().isEmpty()) return text;

        String resolvedText = text;
        for (Map.Entry<CellAddress, String> entry : realTokens.entrySet()) {
            String tmpToken = String.format("[[IMG_TMP:%d:%d]]", entry.getKey().row(), entry.getKey().col());
            if (resolvedText.contains(tmpToken)) {
                resolvedText = resolvedText.replace(tmpToken, entry.getValue());
            }
        }
        return resolvedText;
    }

    private String resolveTestName(String providedName, String fileName) {
        if (providedName != null && !providedName.trim().isEmpty()) return providedName.trim();
        if (fileName == null) return "Uploaded Test - " + LocalDateTime.now();
        return fileName.replaceAll("(?i)\\.xlsx?$", "");
    }

    public void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new ExcelValidationException(List.of("Excel file is required"));
        }

        String fileName = file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new ExcelValidationException(List.of("Only .xlsx or .xls files are supported"));
        }
    }

    private Map<CellAddress, ExtractedImage> extractImages(Sheet sheet, List<String> errors) {
        Map<CellAddress, ExtractedImage> imagesByCell = new HashMap<>();

        if (!(sheet instanceof XSSFSheet xssfSheet)) return imagesByCell;

        XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
        if (drawing == null) return imagesByCell;

        for (XSSFShape shape : drawing.getShapes()) {
            if (!(shape instanceof XSSFPicture picture)) continue;

            XSSFClientAnchor anchor = (XSSFClientAnchor) picture.getClientAnchor();
            int row = anchor.getRow1();
            int col = anchor.getCol1();
            int displayRow = row + 1;

            if (anchor.getRow1() != anchor.getRow2() || anchor.getCol1() != anchor.getCol2()) {
                errors.add("Row " + displayRow + ": Image overlaps multiple cells. Please resize it to fit exactly inside its cell.");
                continue;
            }

            CellAddress address = new CellAddress(row, col);
            if (imagesByCell.containsKey(address)) {
                errors.add("Row " + displayRow + ": Multiple images found in the same cell. Please use only one image per cell.");
                continue;
            }

            XSSFPictureData pictureData = picture.getPictureData();
            String extension = pictureData.suggestFileExtension().toLowerCase(Locale.ROOT);
            imagesByCell.put(address, new ExtractedImage(extension, resolveContentType(extension), pictureData.getData()));
        }

        return imagesByCell;
    }

    private String resolveContentType(String extension) {
        if ("png".equals(extension)) return "image/png";
        if ("jpg".equals(extension) || "jpeg".equals(extension)) return "image/jpeg";
        return "application/octet-stream";
    }

    private static final class ExcelValidationException extends RuntimeException {
        private final List<String> errors;
        private ExcelValidationException(List<String> errors) {
            super(errors.stream().collect(Collectors.joining(", ")));
            this.errors = errors;
        }
        private List<String> getErrors() { return errors; }
    }

    private record CellAddress(int row, int col) {}
    private record ExtractedImage(String extension, String contentType, byte[] data) {}
    private record ParsedData(List<RecruitTestQuestion> questions, Map<CellAddress, ExtractedImage> images) {}
}