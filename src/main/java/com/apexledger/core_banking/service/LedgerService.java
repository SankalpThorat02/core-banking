package com.apexledger.core_banking.service;

import com.apexledger.core_banking.entity.Account;
import com.apexledger.core_banking.entity.Transaction;
import com.apexledger.core_banking.repository.AccountRepository;
import com.apexledger.core_banking.repository.TransactionRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public LedgerService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction transferFunds(String fromAccount, String toAccount, BigDecimal amount, String idempotencyKey) {

        if(transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new RuntimeException("Duplicate transaction request detected !");
        }

        Account sourceAccount = accountRepository.findByAccountNumberForUpdate(fromAccount)
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Account destinationAccount = accountRepository.findByAccountNumberForUpdate(toAccount)
                .orElseThrow(() -> new RuntimeException("Destination account not found"));

        if(!sourceAccount.getStatus().equals("ACTIVE") || !destinationAccount.getStatus().equals("ACTIVE")) {
            throw new RuntimeException("One or both accounts are not active");
        }

        BigDecimal totalCredits = transactionRepository.calculateTotalCredits(sourceAccount);
        BigDecimal totalDebits = transactionRepository.calculateTotalDebits(sourceAccount);
        BigDecimal currentBalance = totalCredits.subtract(totalDebits);

        boolean isVault = sourceAccount.getAccountNumber().equals("VAULT-000");

        if(!isVault && currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setSourceAccount(sourceAccount);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setStatus("COMPLETED");

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction depositFunds(String toAccount, BigDecimal amount, String idempotencyKey) {
        return transferFunds("VAULT-000", toAccount, amount, idempotencyKey);
    }
}
