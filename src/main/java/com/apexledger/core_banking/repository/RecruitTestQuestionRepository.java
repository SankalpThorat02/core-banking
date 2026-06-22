package com.apexledger.core_banking.repository;

import com.apexledger.core_banking.entity.RecruitTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruitTestQuestionRepository extends JpaRepository<RecruitTestQuestion, Long> {

    @Query(value = "SELECT * FROM ( " +
            "    SELECT * FROM sankalp_test_questions " +
            "    WHERE test_id = :testId " +
            "    AND question_level = :level " +
            "    AND question_id NOT IN (:excludedIds) " +
            "    ORDER BY DBMS_RANDOM.VALUE " +
            ") WHERE ROWNUM = 1",
            nativeQuery = true)
    Optional<RecruitTestQuestion> findNextAdaptiveQuestion(
            @Param("testId") Long testId,
            @Param("level") String level,
            @Param("excludedIds") List<Long> excludedIds);
}
