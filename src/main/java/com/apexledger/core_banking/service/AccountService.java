package com.apexledger.core_banking.service;

import com.apexledger.core_banking.model.Account;
import com.apexledger.core_banking.repository.AccountRepository;
import com.apexledger.core_banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final Random random = new Random();

    public Account createAccount(String currency) {
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());

        account.setCurrency(currency.toUpperCase());
        account.setStatus("ACTIVE");

        return accountRepository.save(account);
    }

    private String generateAccountNumber() {
        int randomNumber = 10000000 + random.nextInt(90000000);
        return "ACC-" + randomNumber;
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public BigDecimal calculateCurrentBalance(Account account) {
        BigDecimal totalCredits = transactionRepository.calculateTotalCredits(account);
        BigDecimal totalDebits = transactionRepository.calculateTotalDebits(account);

        return totalCredits.subtract(totalDebits);
    }
}
