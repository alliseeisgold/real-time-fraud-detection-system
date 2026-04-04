package com.frauddetection.fraudanalyzer.controller;

import com.frauddetection.common.ApiResponse;
import com.frauddetection.fraudanalyzer.dto.FraudAlertResponse;
import com.frauddetection.fraudanalyzer.service.FraudAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fraud-alerts")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    @GetMapping("/transaction/{transactionId}")
    public ApiResponse<FraudAlertResponse> getByTransactionId(@PathVariable UUID transactionId) {
        return ApiResponse.success(fraudAlertService.getByTransactionId(transactionId));
    }

    @GetMapping("/recent")
    public ApiResponse<List<FraudAlertResponse>> getRecentAlerts() {
        return ApiResponse.success(fraudAlertService.getRecentAlerts());
    }
}
