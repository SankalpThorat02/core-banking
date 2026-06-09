package com.apexledger.core_banking.service;

import com.apexledger.core_banking.dto.AccountRequest;
import com.apexledger.core_banking.dto.AccountResponse;
import com.apexledger.core_banking.dto.TransactionHistoryResponse;
import com.apexledger.core_banking.model.Account;
import com.apexledger.core_banking.model.AppUser;
import com.apexledger.core_banking.model.Transaction;
import com.apexledger.core_banking.repository.AccountRepository;
import com.apexledger.core_banking.repository.TransactionRepository;
import com.apexledger.core_banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final Random random = new Random();

    public AccountResponse createAccount(AccountRequest accountRequest, String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        String accountNumber = generateAccountNumber();

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCurrency(accountRequest.getCurrency().toUpperCase());
        account.setStatus("Active");

        account.setUser(user);

        Account savedAccount = accountRepository.save(account);

        return AccountResponse.builder()
                .accountNumber(savedAccount.getAccountNumber())
                .currency(savedAccount.getCurrency())
                .status(savedAccount.getStatus())
                .build();
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

    public Page<TransactionHistoryResponse> getAccountStatement(String accountNumber, Pageable pageable) {
        Account account = getAccountByNumber(accountNumber);

        Page<Transaction> transactions = transactionRepository.findAccountStatement(account, pageable);

        return transactions.map(
                txn -> {
                    boolean isSender = txn.getSourceAccount().getAccountNumber().equals(accountNumber);

                    String type = isSender ? "DEBIT" : "CREDIT";
                    String counterpartyAccount = isSender ?
                            txn.getDestinationAccount().getAccountNumber() : txn.getSourceAccount().getAccountNumber();

                    return new TransactionHistoryResponse(
                            txn.getUuid().toString(),
                            type,
                            txn.getAmount(),
                            counterpartyAccount,
                            txn.getTimestamp(),
                            txn.getStatus()
                    );
                }
        );
    }

    public List<AccountResponse> getMyAccounts(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getAccounts().stream()
                .map(account -> AccountResponse.builder()
                        .accountNumber(account.getAccountNumber())
                        .currency(account.getCurrency())
                        .status(account.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
