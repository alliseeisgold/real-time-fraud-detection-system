package com.frauddetection.dashboard.repository;

import com.frauddetection.common.FraudAlertEvent;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.common.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class DashboardProjectionRepository {

    private static final String INSERT_TRANSACTION = """
            INSERT INTO dashboard_transactions (
                transaction_id,
                account_id,
                status,
                amount,
                currency,
                country,
                merchant_category,
                event_time
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_FRAUD_ALERT = """
            INSERT INTO dashboard_fraud_alerts (
                transaction_id,
                account_id,
                reason,
                risk_score,
                detected_at
            )
            VALUES (?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void insertTransaction(TransactionEvent event, TransactionStatus status) {
        jdbcTemplate.update(
                INSERT_TRANSACTION,
                event.transactionId(),
                event.accountId(),
                status.name(),
                event.amount(),
                event.currency(),
                event.country(),
                event.merchantCategory(),
                toTimestamp(event.timestamp())
        );
    }

    public void insertFraudTransaction(FraudAlertEvent event) {
        jdbcTemplate.update(
                INSERT_TRANSACTION,
                event.transactionId(),
                event.accountId(),
                TransactionStatus.FRAUD.name(),
                null,
                null,
                null,
                null,
                toTimestamp(event.detectedAt())
        );
    }

    public void insertFraudAlert(FraudAlertEvent event) {
        jdbcTemplate.update(
                INSERT_FRAUD_ALERT,
                event.transactionId(),
                event.accountId(),
                event.reason(),
                event.riskScore(),
                toTimestamp(event.detectedAt())
        );
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
