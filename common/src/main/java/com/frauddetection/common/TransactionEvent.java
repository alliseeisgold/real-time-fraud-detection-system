package com.frauddetection.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String country,
        Instant timestamp,
        String merchantCategory
) {
}
