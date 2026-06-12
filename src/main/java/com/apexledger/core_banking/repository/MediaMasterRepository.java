package com.apexledger.core_banking.repository;

import com.apexledger.core_banking.entity.MediaMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaMasterRepository extends JpaRepository<MediaMaster, Long> {
}
