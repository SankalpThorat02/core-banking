package com.apexledger.core_banking.repository;

import com.apexledger.core_banking.entity.RecruitTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitTestQuestionRepository extends JpaRepository<RecruitTestQuestion, Long> {
}
