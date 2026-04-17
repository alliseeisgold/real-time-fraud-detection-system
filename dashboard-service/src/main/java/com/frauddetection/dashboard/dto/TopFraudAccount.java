package com.frauddetection.dashboard.dto;

import java.util.UUID;

public record TopFraudAccount(
        UUID accountId,
        long fraudCount
) {
}
