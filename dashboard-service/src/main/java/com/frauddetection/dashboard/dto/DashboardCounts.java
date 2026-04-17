package com.frauddetection.dashboard.dto;

public record DashboardCounts(
        long totalTransactions,
        long fraudCount,
        long verifiedCount
) {
}
