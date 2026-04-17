package com.frauddetection.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record RecentFraudAlertResponse(
        UUID transactionId,
        UUID accountId,
        String reason,
        int riskScore,
        Instant detectedAt
) {
}
