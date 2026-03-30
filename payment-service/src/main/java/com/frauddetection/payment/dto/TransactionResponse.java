package com.frauddetection.payment.dto;

import com.frauddetection.common.Currency;
import com.frauddetection.common.TransactionStatus;
import com.frauddetection.payment.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        BigDecimal amount,
        Currency currency,
        String country,
        String merchantCategory,
        TransactionStatus status,
        Instant createdAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCountry(),
                transaction.getMerchantCategory(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
