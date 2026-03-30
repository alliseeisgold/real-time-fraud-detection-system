package com.frauddetection.payment.repository;

import com.frauddetection.payment.entity.OutboxEvent;
import com.frauddetection.payment.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    Optional<OutboxEvent> findByAggregateId(UUID aggregateId);
}
