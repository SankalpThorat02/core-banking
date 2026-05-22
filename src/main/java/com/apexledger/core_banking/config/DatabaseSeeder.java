package com.apexledger.core_banking.config;

import com.apexledger.core_banking.model.Account;
import com.apexledger.core_banking.model.Transaction;
import com.apexledger.core_banking.repository.AccountRepository;
import com.apexledger.core_banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public void run(String... args) throws Exception {

        if(accountRepository.count() == 0) {

            System.out.println("Seeding initial database record");

            Account vault = new Account();
            vault.setAccountNumber("VAULT-000");
            vault.setCurrency("INR");
            vault.setStatus("ACTIVE");
            accountRepository.save(vault);


            Account sankalp = new Account();
            sankalp.setAccountNumber("ACC-1001");
            sankalp.setCurrency("INR");
            sankalp.setStatus("ACTIVE");
            accountRepository.save(sankalp);

            Account random = new Account();
            random.setAccountNumber("ACC-1002");
            random.setCurrency("INR");
            random.setStatus("ACTIVE");
            accountRepository.save(random);


            Transaction genesisTxn = new Transaction();
            genesisTxn.setSourceAccount(vault);
            genesisTxn.setDestinationAccount(sankalp);
            genesisTxn.setAmount(new BigDecimal("1000.00"));
            genesisTxn.setStatus("COMPLETED");
            genesisTxn.setTimestamp(LocalDateTime.now());
            genesisTxn.setIdempotencyKey(UUID.randomUUID().toString());

            transactionRepository.save(genesisTxn);

            System.out.println("Database seeded successful");
        }
    }
}
