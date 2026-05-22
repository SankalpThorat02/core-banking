package com.apexledger.core_banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateAccountResponse {

    private String accountNumber;
    private String currency;
    private String status;
}
