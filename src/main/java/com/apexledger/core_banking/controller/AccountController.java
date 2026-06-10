package com.apexledger.core_banking.controller;

import com.apexledger.core_banking.dto.*;
import com.apexledger.core_banking.entity.Account;
import com.apexledger.core_banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody AccountRequest request,
            Principal principal) {

        AccountResponse response = accountService.createAccount(request, principal.getName());
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

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<Page<TransactionHistoryResponse>> getStatement(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<TransactionHistoryResponse> statement = accountService.getAccountStatement(accountNumber, pageable);
        return ResponseEntity.ok(statement);
    }

    @GetMapping("/myAccounts")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Principal principal) {
        String username = principal.getName();

        return ResponseEntity.ok(accountService.getMyAccounts(username));
    }
}
