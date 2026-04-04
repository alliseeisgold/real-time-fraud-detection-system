package com.frauddetection.fraudanalyzer.dto;

import com.frauddetection.fraudanalyzer.entity.FraudAlert;

import java.time.Instant;
import java.util.UUID;

public record FraudAlertResponse(
        UUID id,
        UUID transactionId,
        UUID accountId,
        String reason,
        int riskScore,
        Instant detectedAt
) {

    public static FraudAlertResponse from(FraudAlert fraudAlert) {
        return new FraudAlertResponse(
                fraudAlert.getId(),
                fraudAlert.getTransactionId(),
                fraudAlert.getAccountId(),
                fraudAlert.getReason(),
                fraudAlert.getRiskScore(),
                fraudAlert.getDetectedAt()
        );
    }
}
