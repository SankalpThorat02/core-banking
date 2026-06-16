package com.apexledger.core_banking.controller;

import com.apexledger.core_banking.service.TestService;
import com.apexledger.core_banking.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/test")
public class TestUploadController {

    @Autowired
    private TestService testService;

    @PostMapping(value = "/create-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> createTest(
            @RequestParam(value = "testName", required = false) String testName,
            @RequestParam(value = "subjective", required = false) MultipartFile subjectiveFile,
            @RequestParam(value = "objective", required = false) MultipartFile objectiveFile) {

        boolean hasObjective =  objectiveFile != null && !objectiveFile.isEmpty();
        boolean hasSubjective =  subjectiveFile != null && !subjectiveFile.isEmpty();

        if (!hasObjective && !hasSubjective) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>("ERROR", "Please upload at least one test file (Objective or Subjective).", null)
            );
        }

        ApiResponse<?> response = testService.createTest(testName, objectiveFile, subjectiveFile);

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(response);
    }
}
