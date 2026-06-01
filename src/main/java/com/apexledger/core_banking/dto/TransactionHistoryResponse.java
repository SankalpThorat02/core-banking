package com.apexledger.core_banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionHistoryResponse {
    private String transactionId;
    private String type;
    private BigDecimal amount;
    private String counterpartyAccount;
    private LocalDateTime timestamp;
    private String status;
}
