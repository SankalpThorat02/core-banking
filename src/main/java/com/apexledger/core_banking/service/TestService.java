package com.apexledger.core_banking.service;

import com.apexledger.core_banking.entity.RecruitTest;
import com.apexledger.core_banking.entity.RecruitTestQuestion;
import com.apexledger.core_banking.util.ApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    public void validateUploadFile(MultipartFile file) {

        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new ExcelValidationException(List.of("Excel file is required"));
        }

        String fileName = file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new ExcelValidationException(List.of("Only .xlsx or .xls files are supported"));
        }
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
}
