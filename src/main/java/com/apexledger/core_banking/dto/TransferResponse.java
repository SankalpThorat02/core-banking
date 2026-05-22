package com.apexledger.core_banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransferResponse {

    private String transactionId;
    private String status;
    private BigDecimal amount;
    private LocalDateTime timestamp;


}
