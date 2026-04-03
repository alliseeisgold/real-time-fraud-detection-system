package com.frauddetection.fraudanalyzer.dto;

import com.frauddetection.common.FraudReason;

import java.util.List;

public record AnalysisResult(
        boolean fraud,
        List<FraudReason> reasons,
        int riskScore
) {
}
