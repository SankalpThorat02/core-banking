package com.apexledger.core_banking.dto;

import lombok.Data;

@Data
public class QuestionDTO {
    private Long questionId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String difficultyLevel;

}