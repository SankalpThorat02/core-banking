package com.apexledger.core_banking.dto;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private Long candidateId;
    private Long testId;
    private Long questionId;
    private String selectedOption;
}
