package com.apexledger.core_banking.controller;

import com.apexledger.core_banking.dto.CreateTestRequest;
import com.apexledger.core_banking.dto.SubmitAnswerRequest;
import com.apexledger.core_banking.dto.SubmitAnswerResponse;
import com.apexledger.core_banking.service.AdaptiveTestService;
import com.apexledger.core_banking.service.TestService;
import com.apexledger.core_banking.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/tests")
public class TestController {

    @Autowired
    private TestService testService;

    @Autowired
    private AdaptiveTestService adaptiveTestService;

    @PostMapping(value = "/create-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> createTest(
            @ModelAttribute CreateTestRequest request,
            @RequestPart(value = "objectiveFile", required = false) MultipartFile objectiveFile,
            @RequestPart(value = "subjectiveFile", required = false) MultipartFile subjectiveFile) {

        ApiResponse<?> response = testService.createTest(request, objectiveFile, subjectiveFile);

        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllTests() {
        ApiResponse<?> response = testService.getAllTestSummaries();
        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{testId}")
    public ResponseEntity<ApiResponse<?>> getTestDetails(@PathVariable Long testId) {
        ApiResponse<?> response = testService.getTestDetails(testId);
        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start")
    public ResponseEntity<Object> startTest(
            @RequestParam("candidateId") Long candidateId,
            @RequestParam("testId") Long testId) {

        Object firstQuestion = adaptiveTestService.startTest(candidateId, testId);
        return ResponseEntity.ok(firstQuestion);
    }

    @PostMapping("/submit")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @RequestBody SubmitAnswerRequest request) {

        SubmitAnswerResponse response = adaptiveTestService.submitAnswer(request);
        return ResponseEntity.ok(response);
    }
}
