package com.apexledger.core_banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Source account number is required")
    private String fromAccount;

    @NotBlank(message = "Destination account number is required")
    private String toAccount;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least 1 paise")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required to prevent duplicate charges")
    private String idempotencyKey;
}
