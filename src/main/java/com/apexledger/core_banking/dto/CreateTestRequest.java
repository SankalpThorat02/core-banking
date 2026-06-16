package com.apexledger.core_banking.dto;

import lombok.Data;

@Data
public class CreateTestRequest {

    private String testName;
    private Long designationId;
    private String uploadedBy;
    private String testType;
    private Double testPassingPercentage;
    private Double objectivePassingPercentage;
    private Double subjectivePassingPercentage;
}
