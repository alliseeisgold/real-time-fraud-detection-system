package com.frauddetection.common;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID transactionId,
        UUID accountId,
        NotificationType type,
        String message,
        Instant createdAt
) {
}
