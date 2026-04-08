package com.frauddetection.notification.repository;

import com.frauddetection.common.NotificationType;
import com.frauddetection.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByTransactionIdAndType(UUID transactionId, NotificationType type);

    List<Notification> findByAccountIdOrderBySentAtDesc(UUID accountId);
}
