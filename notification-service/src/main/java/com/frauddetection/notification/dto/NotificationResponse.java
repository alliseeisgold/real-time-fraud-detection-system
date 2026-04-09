package com.frauddetection.notification.dto;

import com.frauddetection.common.NotificationType;
import com.frauddetection.notification.entity.Notification;
import com.frauddetection.notification.entity.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID transactionId,
        UUID accountId,
        NotificationType type,
        String message,
        Instant sentAt,
        NotificationStatus status
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTransactionId(),
                notification.getAccountId(),
                notification.getType(),
                notification.getMessage(),
                notification.getSentAt(),
                notification.getStatus()
        );
    }
}
