package com.apexledger.core_banking.controller;

import com.apexledger.core_banking.dto.BalanceResponse;
import com.apexledger.core_banking.dto.CreateAccountRequest;
import com.apexledger.core_banking.dto.CreateAccountResponse;
import com.apexledger.core_banking.model.Account;
import com.apexledger.core_banking.repository.AccountRepository;
import com.apexledger.core_banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(@Valid @RequestBody CreateAccountResponse request) {

        Account savedAccount = accountService.createAccount(request.getCurrency());

        CreateAccountResponse response = new CreateAccountResponse(
                savedAccount.getAccountNumber(),
                savedAccount.getCurrency(),
                savedAccount.getStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountNumber) {
        Account account = accountService.getAccountByNumber(accountNumber);

        BigDecimal currentBalance = accountService.calculateCurrentBalance(account);
        BalanceResponse response = new BalanceResponse(
                account.getAccountNumber(),
                currentBalance,
                account.getCurrency(),
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }
}
