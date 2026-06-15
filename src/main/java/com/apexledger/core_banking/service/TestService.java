package com.apexledger.core_banking.service;

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

    @Transactional
    public ApiResponse<?> createTest(String testName, MultipartFile file) {
        try {
            //1. Validate file
            validateUploadFile(file);

            String finalTestName = resolveTestName(testName, file.getOriginalFilename());

            RecruitTest test = RecruitTest.builder()
                    .testName(finalTestName)
                    .sourceFileName(file.getOriginalFilename())
                    .createdAt(LocalDateTime.now())
                    .build();

            //Saving Test
            RecruitTest savedTest = recruitTestRepository.save(test);

            ParsedData extractedData = readQuestionsFromExcel(file, savedTest);

            //Saving Media
            persistImagesAndResolveTokens(extractedData.questions(), extractedData.images());

            //Saving test questions
            recruitTestQuestionRepository.saveAll(extractedData.questions());


            return new ApiResponse<>("SUCCESS", "Test parsed Successfully with " + extractedData.questions.size() + " questions", null);
        } catch (ExcelValidationException ex){
            return new ApiResponse<>("ERROR", "Excel Validation Error", ex.getErrors());

        } catch (Exception ex) {
            return new ApiResponse<>("ERROR", "An unexpected error occurred: " + ex.getMessage(), null);
        }
    }

    public ParsedData readQuestionsFromExcel(MultipartFile file, RecruitTest test) throws IOException {
        List<String> errors = new ArrayList<>();
        List<RecruitTestQuestion> questions = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())){
            Sheet sheet = workbook.getSheetAt(0);

            DataFormatter formatter = new DataFormatter();

            if(sheet == null || sheet.getLastRowNum() <= 0){
                throw new ExcelValidationException(List.of("Excel file does not contain any question rows."));

            }

            Map<CellAddress, ExtractedImage> imagesByCell = extractImages(sheet, errors);

            for(int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++){
                Row row = sheet.getRow(rowIndex);

                //Unchecked
                if(isRowEmpty(row, formatter) && !imagesByCell.containsKey(new CellAddress(rowIndex,1))) {
                    continue;
                }

                RecruitTestQuestion question = mapQuestion(row, rowIndex, formatter, imagesByCell, errors);
                if(question != null) {
                   test.addQuestions(question);
                    questions.add(question);
                }
            }

            if(questions.isEmpty() && errors.isEmpty()){
                errors.add("Excel file does not contain any valid questions.");
            }

            if(!errors.isEmpty()){
                throw new ExcelValidationException(errors);
            }

            System.out.println("Successfully parsed " + questions.size() + " questions and saved " + imagesByCell.size() + " images.");
            return new ParsedData(questions, imagesByCell);
        }
    }

    private RecruitTestQuestion mapQuestion(Row row, int rowIndex, DataFormatter formatter, Map<CellAddress, ExtractedImage> imagesByCell, List<String> errors) {

        int displayRow = rowIndex + 1;  // For user-friendly error message

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

        Double marks = 0.0;
        try {
            marks = Double.parseDouble(marksStr);
        } catch (NumberFormatException e) {
            errors.add("Row " + displayRow + ": Marks must be a number.");
            return null;
        }

        return RecruitTestQuestion.builder()
                .rowNo(displayRow)
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

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null)
            return "";

        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
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

        return text + " \n " + token;
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null)
            return true;

        for (int i = 0; i <= 8; i++) {
            if (!getCellValue(row, i, formatter).isEmpty()) return false;
        }
        return true;
    }


    //Save images and Swap tokens
    private int persistImagesAndResolveTokens(List<RecruitTestQuestion> questions,
                                              Map<CellAddress, ExtractedImage> imagesByCell) {

        Map<CellAddress, String> realTokens = new HashMap<>();

        for(Map.Entry<CellAddress, ExtractedImage> entry : imagesByCell.entrySet()) {
            CellAddress address = entry.getKey();
            ExtractedImage image = entry.getValue();

            String generatedFileName = String.format("excel-img-r%d-c%d.%s",
                    (address.row() + 1), (address.col() + 1), image.extension());

            MediaMaster media = MediaMaster.builder()
                    .fileName(generatedFileName)
                    .contentType(image.contentType())
                    .fileSize((long) image.data().length)
                    .fileData(image.data())
                    .createdAt(LocalDateTime.now())
                    .build();

            media = mediaMasterRepository.save(media);

            String finalToken = "[[IMG:" + media.getId() + "]]";
            realTokens.put(address, finalToken);
        }

        for(RecruitTestQuestion question : questions) {
            question.setQuestion(swapTempTokens(question.getQuestion(), realTokens));
            question.setOptionA(swapTempTokens(question.getOptionA(), realTokens));
            question.setOptionB(swapTempTokens(question.getOptionB(), realTokens));
            question.setOptionC(swapTempTokens(question.getOptionC(), realTokens));
            question.setOptionD(swapTempTokens(question.getOptionD(), realTokens));
        }

        return realTokens.size();
    }

    private String swapTempTokens(String text, Map<CellAddress, String> realTokens) {
        if(text == null || text.trim().isEmpty()) {
            return text;
        }

        String resolvedText = text;

        for (Map.Entry<CellAddress, String> entry : realTokens.entrySet()) {
            CellAddress address = entry.getKey();
            String realToken = entry.getValue();

            String tmpToken = String.format("[[IMG_TMP:%d:%d]]", address.row(), address.col());

            if (resolvedText.contains(tmpToken)) {
                resolvedText = resolvedText.replace(tmpToken, realToken);
            }
        }

        return resolvedText;


    }

    private String resolveTestName(String providedName, String fileName) {
        if(providedName != null && !providedName.trim().isEmpty()) {
            return providedName.trim();
        }

        if(fileName == null) {
            return "Uploaded Test - " + LocalDateTime.now();
        }

        return fileName.replaceAll("(?i)\\.xlsx?$", "");
    }

    // Check for extensions and if file empty
    public void validateUploadFile(MultipartFile file) {

        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new ExcelValidationException(List.of("Excel file is required"));
        }

        String fileName = file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new ExcelValidationException(List.of("Only .xlsx or .xls files are supported"));
        }
    }

    //Extract Images from file
    private Map<CellAddress, ExtractedImage> extractImages(Sheet sheet, List<String> errors) {
        Map<CellAddress, ExtractedImage> imagesByCell = new HashMap<>();

        if(!(sheet instanceof XSSFSheet xssfSheet)) {
            return imagesByCell;
        }

        XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
        if(drawing == null) {
            return imagesByCell;
        }

        for(XSSFShape shape : drawing.getShapes()) {
            if(!(shape instanceof XSSFPicture picture)) {
                continue;
            }

            XSSFClientAnchor anchor = (XSSFClientAnchor) picture.getClientAnchor();

            int row = anchor.getRow1();
            int col = anchor.getCol1();
            int displayRow = row + 1; // For user-friendly error messages (Row 0 is Row 1 to users)


            // -------- Validations ---------
            //Image cell validation
            if(anchor.getRow1() != anchor.getRow2() || anchor.getCol1() != anchor.getCol2()) {
                errors.add("Row " + displayRow + ": Image overlaps multiple cells. Please resize it to fit exactly inside its cell.");
                continue;
            }

            //Multiple Images in one cell validation
            CellAddress address = new CellAddress(row, col);
            if(imagesByCell.containsKey(address)) {
                errors.add("Row " + displayRow + ": Multiple images found in the same cell. Please use only one image per cell.");
                continue;
            }

            XSSFPictureData pictureData = picture.getPictureData();
            String extension = pictureData.suggestFileExtension().toLowerCase(Locale.ROOT);
            byte[] data = pictureData.getData();
            String contentType = resolveContentType(extension);

            imagesByCell.put(address, new ExtractedImage(extension, contentType, data));
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

        private List<String> getErrors() {
            return errors;
        }
    }


    private record CellAddress(int row, int col) {}
    private record ExtractedImage(String extension, String contentType, byte[] data) {}

    private record ParsedData(List<RecruitTestQuestion> questions, Map<CellAddress, ExtractedImage> images) {}
}
