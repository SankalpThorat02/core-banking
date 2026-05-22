package com.apexledger.core_banking.service;

import com.apexledger.core_banking.model.Account;
import com.apexledger.core_banking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
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
}
