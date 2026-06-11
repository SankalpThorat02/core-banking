package com.apexledger.core_banking.service;

import com.apexledger.core_banking.entity.RecruitTest;
import com.apexledger.core_banking.entity.RecruitTestQuestion;
import com.apexledger.core_banking.util.ApiResponse;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestService {


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


            List<RecruitTestQuestion> questions = readQuestionsFromExcel(file, testName);


            return new ApiResponse<>("SUCCESS", "Test parsed Successfully with " + questions.size() + " questions", null);
        } catch (ExcelValidationException ex){
            return new ApiResponse<>("ERROR", "Excel Validation Error", ex.getErrors());

        } catch (Exception ex) {
            return new ApiResponse<>("ERROR", "An unexpected error occurred: " + ex.getMessage(), null);
        }
    }

    private String resolveTestName(String providedName, String fileName) {
        if(providedName != null && !providedName.trim().isEmpty()) {
            return providedName.trim();
        }

        if(fileName == null) {
            return "Uploaded Test - " + LocalDateTime.now().toString();
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

    public List<RecruitTestQuestion> readQuestionsFromExcel(MultipartFile file, String testName) {
        List<RecruitTestQuestion> questions = new ArrayList<>();
        return questions;
    }

    private record CellAddress(int row, int col) {}
    private record ExtractedImage(String extension, String contentType, byte[] data) {}


}
