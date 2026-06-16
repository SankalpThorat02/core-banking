package com.apexledger.core_banking.repository;

import com.apexledger.core_banking.dto.TestSummary;
import com.apexledger.core_banking.entity.RecruitTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruitTestRepository extends JpaRepository<RecruitTest, Long> {

    @Query("""
        SELECT new com.apexledger.core_banking.dto.TestSummary(
            t.id, t.testName, t.uploadedDate
        )
        FROM RecruitTest t
        ORDER BY t.uploadedDate DESC
    """)
    List<TestSummary> findAllTestSummaries();

    @Query("SELECT t FROM RecruitTest t LEFT JOIN FETCH t.questions WHERE t.id = :testId")
    Optional<RecruitTest> findByIdWithQuestions(@Param("testId") Long testId);
}
