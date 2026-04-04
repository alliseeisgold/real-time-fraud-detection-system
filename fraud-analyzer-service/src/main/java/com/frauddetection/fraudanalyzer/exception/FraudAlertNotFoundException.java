package com.frauddetection.fraudanalyzer.exception;

import java.util.UUID;

public class FraudAlertNotFoundException extends RuntimeException {

    public FraudAlertNotFoundException(UUID transactionId) {
        super("Fraud alert not found for transactionId=%s".formatted(transactionId));
    }
}
