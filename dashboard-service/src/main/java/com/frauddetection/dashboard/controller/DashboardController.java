package com.frauddetection.dashboard.controller;

import com.frauddetection.common.ApiResponse;
import com.frauddetection.dashboard.dto.DashboardStats;
import com.frauddetection.dashboard.dto.RecentFraudAlertResponse;
import com.frauddetection.dashboard.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/stats")
    public ApiResponse<DashboardStats> getStats() {
        return ApiResponse.success(dashboardStatsService.getStats());
    }

    @GetMapping("/recent-frauds")
    public ApiResponse<List<RecentFraudAlertResponse>> getRecentFrauds() {
        return ApiResponse.success(dashboardStatsService.getRecentFrauds());
    }
}
