package com.frauddetection.payment.exception;

public class OutboxSerializationException extends RuntimeException {

    public OutboxSerializationException(Throwable cause) {
        super("Failed to serialize transaction event for outbox", cause);
    }
}
