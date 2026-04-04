package com.frauddetection.fraudanalyzer.service;

import com.frauddetection.common.TransactionEvent;
import com.frauddetection.fraudanalyzer.dto.AnalysisResult;
import com.frauddetection.fraudanalyzer.entity.FraudAlert;
import com.frauddetection.fraudanalyzer.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;

    @Transactional
    public FraudAlert saveAlert(TransactionEvent event, AnalysisResult result) {
        return fraudAlertRepository.findByTransactionId(event.transactionId())
                .orElseGet(() -> fraudAlertRepository.save(FraudAlert.builder()
                        .transactionId(event.transactionId())
                        .accountId(event.accountId())
                        .reason(formatReasons(result))
                        .riskScore(result.riskScore())
                        .build()));
    }

    private String formatReasons(AnalysisResult result) {
        return result.reasons()
                .stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}
