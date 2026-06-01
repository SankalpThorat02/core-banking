package com.apexledger.core_banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BalanceResponse {
    private String accountNumber;
    private BigDecimal currentBalance;
    private String currency;
    private LocalDateTime calculatedAt;
}
