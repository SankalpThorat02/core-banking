package com.apexledger.core_banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class DepositRequest {

    @NotBlank(message = "Destination account number is required")
    private String toAccount;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Deposit amount must be atleast 1 paise")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

}
