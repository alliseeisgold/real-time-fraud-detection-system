package com.frauddetection.dashboard.service;

import com.frauddetection.common.FraudAlertEvent;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.common.TransactionStatus;
import com.frauddetection.dashboard.repository.DashboardProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardProjectionService {

    private final DashboardProjectionRepository dashboardProjectionRepository;

    /**
     * Records an approved transaction in the dashboard read model.
     *
     * @param event verified transaction event consumed from Kafka
     */
    public void recordVerifiedTransaction(TransactionEvent event) {
        dashboardProjectionRepository.insertTransaction(event, TransactionStatus.VERIFIED);
        log.info("Recorded verified transaction projection transactionId={}", event.transactionId());
    }

    /**
     * Records a fraud alert and its transaction marker in the dashboard read model.
     *
     * @param event fraud alert event consumed from Kafka
     */
    public void recordFraudAlert(FraudAlertEvent event) {
        dashboardProjectionRepository.insertFraudTransaction(event);
        dashboardProjectionRepository.insertFraudAlert(event);
        log.info("Recorded fraud alert projection transactionId={} riskScore={}",
                event.transactionId(), event.riskScore());
    }
}
