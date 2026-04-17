package com.frauddetection.dashboard.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.FraudAlertEvent;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.dashboard.service.DashboardProjectionService;
import com.frauddetection.dashboard.service.DashboardWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardEventConsumer {

    private static final String FRAUD_TRANSACTIONS_TOPIC = "transactions.fraud";
    private static final String VERIFIED_TRANSACTIONS_TOPIC = "transactions.verified";

    private final ObjectMapper objectMapper;
    private final DashboardProjectionService dashboardProjectionService;
    private final DashboardWebSocketPublisher dashboardWebSocketPublisher;

    @KafkaListener(topics = VERIFIED_TRANSACTIONS_TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void consumeVerifiedTransaction(String rawMessage, Acknowledgment acknowledgment) {
        try {
            TransactionEvent event = objectMapper.readValue(rawMessage, TransactionEvent.class);
            dashboardProjectionService.recordVerifiedTransaction(event);
            acknowledgment.acknowledge();
        } catch (JsonProcessingException exception) {
            log.error("Failed to parse verified transaction event", exception);
            acknowledgment.acknowledge();
        } catch (Exception exception) {
            log.error("Failed to project verified transaction event", exception);
            throw exception;
        }
    }

    @KafkaListener(topics = FRAUD_TRANSACTIONS_TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void consumeFraudAlert(String rawMessage, Acknowledgment acknowledgment) {
        try {
            FraudAlertEvent event = objectMapper.readValue(rawMessage, FraudAlertEvent.class);
            dashboardProjectionService.recordFraudAlert(event);
            dashboardWebSocketPublisher.publishFraudAlert(event);
            acknowledgment.acknowledge();
        } catch (JsonProcessingException exception) {
            log.error("Failed to parse fraud alert event", exception);
            acknowledgment.acknowledge();
        } catch (Exception exception) {
            log.error("Failed to project fraud alert event", exception);
            throw exception;
        }
    }
}
