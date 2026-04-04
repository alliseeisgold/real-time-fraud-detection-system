package com.frauddetection.fraudanalyzer.service;

import com.frauddetection.common.TransactionEvent;
import com.frauddetection.fraudanalyzer.dto.AnalysisResult;
import com.frauddetection.fraudanalyzer.dto.FraudAlertResponse;
import com.frauddetection.fraudanalyzer.entity.FraudAlert;
import com.frauddetection.fraudanalyzer.exception.FraudAlertNotFoundException;
import com.frauddetection.fraudanalyzer.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;

    /**
     * Saves a fraud alert for the analyzed transaction when it does not already exist.
     *
     * @param event source transaction event
     * @param result fraud analysis result
     * @return persisted fraud alert
     */
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

    /**
     * Finds a fraud alert by transaction id and maps it to an API response DTO.
     *
     * @param transactionId transaction id to search by
     * @return fraud alert response
     */
    @Transactional(readOnly = true)
    public FraudAlertResponse getByTransactionId(UUID transactionId) {
        return fraudAlertRepository.findByTransactionId(transactionId)
                .map(FraudAlertResponse::from)
                .orElseThrow(() -> new FraudAlertNotFoundException(transactionId));
    }

    /**
     * Returns the latest fraud alerts for manual smoke testing and dashboard-like checks.
     *
     * @return recent fraud alert responses
     */
    @Transactional(readOnly = true)
    public List<FraudAlertResponse> getRecentAlerts() {
        return fraudAlertRepository.findTop20ByOrderByDetectedAtDesc()
                .stream()
                .map(FraudAlertResponse::from)
                .toList();
    }

    private String formatReasons(AnalysisResult result) {
        return result.reasons()
                .stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}
