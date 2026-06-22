package com.apexledger.core_banking.dto;

import lombok.Data;

@Data
public class SubmitAnswerResponse {
    private boolean isTestCompleted;
    private String completionMessage;

    private QuestionDTO nextQuestion;
}