package com.apexledger.core_banking.service;

import com.apexledger.core_banking.util.ApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TestService {

    public ApiResponse<?> createTest(String testName, MultipartFile file) {

        //1. Validate file
        validateUploadFile(file);

        return new ApiResponse<>("SUCCESS", "Prototype connected. File recieved successfully", null);
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
}
