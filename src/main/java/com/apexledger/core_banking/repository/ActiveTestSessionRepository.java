package com.apexledger.core_banking.repository;

import com.apexledger.core_banking.entity.ActiveTestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActiveTestSessionRepository extends JpaRepository<ActiveTestSession, Long> {

    Optional<ActiveTestSession> findByCandidateIdAndTestIdAndStatus(Long candidateId, Long testId, String status);
}
