package com.frauddetection.dashboard.service;

import com.frauddetection.dashboard.dto.DashboardCounts;
import com.frauddetection.dashboard.dto.DashboardStats;
import com.frauddetection.dashboard.dto.RecentFraudAlertResponse;
import com.frauddetection.dashboard.repository.DashboardQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private static final int TOP_FRAUD_ACCOUNTS_LIMIT = 5;
    private static final int RECENT_FRAUD_ALERTS_LIMIT = 20;

    private final DashboardQueryRepository dashboardQueryRepository;

    /**
     * Builds dashboard statistics from the ClickHouse read model.
     *
     * @return current dashboard statistics
     */
    public DashboardStats getStats() {
        DashboardCounts counts = dashboardQueryRepository.getCounts();
        return new DashboardStats(
                counts.totalTransactions(),
                counts.fraudCount(),
                counts.verifiedCount(),
                calculateFraudRate(counts),
                dashboardQueryRepository.getTopFraudAccounts(TOP_FRAUD_ACCOUNTS_LIMIT)
        );
    }

    /**
     * Returns the latest fraud alerts from the ClickHouse read model.
     *
     * @return recent fraud alerts
     */
    public List<RecentFraudAlertResponse> getRecentFrauds() {
        return dashboardQueryRepository.getRecentFraudAlerts(RECENT_FRAUD_ALERTS_LIMIT);
    }

    private BigDecimal calculateFraudRate(DashboardCounts counts) {
        if (counts.totalTransactions() == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(counts.fraudCount())
                .divide(BigDecimal.valueOf(counts.totalTransactions()), 4, RoundingMode.HALF_UP);
    }
}
