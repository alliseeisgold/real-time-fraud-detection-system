package com.frauddetection.payment.repository;

import com.frauddetection.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
