package com.apexledger.core_banking.dto;

import java.time.LocalDateTime;

public record TestSummary(
        Long testId,
        String testName,
        LocalDateTime uploadedDate
) {}