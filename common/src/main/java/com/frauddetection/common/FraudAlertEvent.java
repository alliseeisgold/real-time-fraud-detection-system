package com.frauddetection.common;

import java.time.Instant;
import java.util.UUID;

public record FraudAlertEvent(
        UUID transactionId,
        UUID accountId,
        String reason,
        int riskScore,
        Instant detectedAt
) {
}
