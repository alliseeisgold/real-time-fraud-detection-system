package com.frauddetection.dashboard.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickHouseSchemaInitializer implements ApplicationRunner {

    private static final String CREATE_DASHBOARD_TRANSACTIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS dashboard_transactions (
                transaction_id UUID,
                account_id UUID,
                status LowCardinality(String),
                amount Nullable(Decimal(19, 2)),
                currency Nullable(String),
                country Nullable(String),
                merchant_category Nullable(String),
                event_time DateTime64(3, 'UTC'),
                received_at DateTime64(3, 'UTC') DEFAULT now64(3)
            )
            ENGINE = ReplacingMergeTree(received_at)
            PARTITION BY toYYYYMM(event_time)
            ORDER BY (transaction_id, status)
            """;

    private static final String CREATE_DASHBOARD_FRAUD_ALERTS_TABLE = """
            CREATE TABLE IF NOT EXISTS dashboard_fraud_alerts (
                transaction_id UUID,
                account_id UUID,
                reason String,
                risk_score UInt16,
                detected_at DateTime64(3, 'UTC'),
                received_at DateTime64(3, 'UTC') DEFAULT now64(3)
            )
            ENGINE = ReplacingMergeTree(received_at)
            PARTITION BY toYYYYMM(detected_at)
            ORDER BY transaction_id
            """;

    private final JdbcTemplate jdbcTemplate;

    @Value("${dashboard.clickhouse.initialize-schema:true}")
    private boolean initializeSchema;

    @Override
    public void run(ApplicationArguments args) {
        if (!initializeSchema) {
            log.info("ClickHouse schema initialization is disabled");
            return;
        }

        jdbcTemplate.execute(CREATE_DASHBOARD_TRANSACTIONS_TABLE);
        jdbcTemplate.execute(CREATE_DASHBOARD_FRAUD_ALERTS_TABLE);
        log.info("ClickHouse dashboard schema initialized");
    }
}
