package com.frauddetection.dashboard.repository;

import com.frauddetection.dashboard.dto.DashboardCounts;
import com.frauddetection.dashboard.dto.RecentFraudAlertResponse;
import com.frauddetection.dashboard.dto.TopFraudAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DashboardQueryRepository {

    private static final String GET_COUNTS = """
            SELECT
                countDistinct(transaction_id) AS total_transactions,
                countIf(status = 'FRAUD') AS fraud_count,
                countIf(status = 'VERIFIED') AS verified_count
            FROM dashboard_transactions FINAL
            """;

    private static final String GET_TOP_FRAUD_ACCOUNTS = """
            SELECT
                account_id,
                count() AS fraud_count
            FROM dashboard_fraud_alerts FINAL
            GROUP BY account_id
            ORDER BY fraud_count DESC, account_id
            LIMIT ?
            """;

    private static final String GET_RECENT_FRAUD_ALERTS = """
            SELECT
                transaction_id,
                account_id,
                reason,
                risk_score,
                detected_at
            FROM dashboard_fraud_alerts FINAL
            ORDER BY detected_at DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public DashboardCounts getCounts() {
        return jdbcTemplate.queryForObject(GET_COUNTS, (resultSet, rowNum) -> new DashboardCounts(
                resultSet.getLong("total_transactions"),
                resultSet.getLong("fraud_count"),
                resultSet.getLong("verified_count")
        ));
    }

    public List<TopFraudAccount> getTopFraudAccounts(int limit) {
        return jdbcTemplate.query(
                GET_TOP_FRAUD_ACCOUNTS,
                (resultSet, rowNum) -> new TopFraudAccount(
                        getUuid(resultSet, "account_id"),
                        resultSet.getLong("fraud_count")
                ),
                limit
        );
    }

    public List<RecentFraudAlertResponse> getRecentFraudAlerts(int limit) {
        return jdbcTemplate.query(
                GET_RECENT_FRAUD_ALERTS,
                (resultSet, rowNum) -> new RecentFraudAlertResponse(
                        getUuid(resultSet, "transaction_id"),
                        getUuid(resultSet, "account_id"),
                        resultSet.getString("reason"),
                        resultSet.getInt("risk_score"),
                        getInstant(resultSet, "detected_at")
                ),
                limit
        );
    }

    private UUID getUuid(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private Instant getInstant(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        return Instant.parse(value.toString());
    }
}
