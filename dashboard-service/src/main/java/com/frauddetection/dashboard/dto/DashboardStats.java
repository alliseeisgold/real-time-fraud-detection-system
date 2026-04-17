package com.frauddetection.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStats(
        long totalTransactions,
        long fraudCount,
        long verifiedCount,
        BigDecimal fraudRate,
        List<TopFraudAccount> topFraudAccounts
) {
}
