package com.apexledger.core_banking.repository;

import com.apexledger.core_banking.model.Account;
import com.apexledger.core_banking.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.servlet.tags.form.SelectTag;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    @Query("SELECT CASE WHEN COUNT (t) > 0 THEN true ELSE false END FROM Transaction t WHERE t.idempotencyKey = :key")
    boolean existsByIdempotencyKey (@Param("key") String idempotencyKey);

    //Total Money credited
    @Query("SELECT COALESCE(SUM (t.amount), 0) FROM Transaction t WHERE t.destinationAccount = :account AND t.status = 'COMPLETED' ")
    BigDecimal calculateTotalCredits(@Param("account") Account account);

    //Total Money debited
    @Query("SELECT COALESCE(SUM (t.amount), 0) FROM Transaction t WHERE t.sourceAccount = :account AND t.status = 'COMPLETED' ")
    BigDecimal calculateTotalDebits(@Param("account") Account account);
}
