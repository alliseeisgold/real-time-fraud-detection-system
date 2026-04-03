package com.frauddetection.fraudanalyzer.repository;

import com.frauddetection.fraudanalyzer.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    Optional<FraudAlert> findByTransactionId(UUID transactionId);

    List<FraudAlert> findTop20ByOrderByDetectedAtDesc();
}
