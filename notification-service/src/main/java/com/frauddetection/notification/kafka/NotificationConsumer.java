package com.frauddetection.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.FraudAlertEvent;
import com.frauddetection.common.NotificationType;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.notification.entity.Notification;
import com.frauddetection.notification.entity.NotificationStatus;
import com.frauddetection.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final String FRAUD_TRANSACTIONS_TOPIC = "transactions.fraud";
    private static final String VERIFIED_TRANSACTIONS_TOPIC = "transactions.verified";

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = FRAUD_TRANSACTIONS_TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void consumeFraudAlert(String rawMessage, Acknowledgment acknowledgment) {
        try {
            FraudAlertEvent event = objectMapper.readValue(rawMessage, FraudAlertEvent.class);
            Notification notification = Notification.builder()
                    .transactionId(event.transactionId())
                    .accountId(event.accountId())
                    .type(NotificationType.FRAUD_ALERT)
                    .message("Fraud detected for transaction %s. Reason: %s. Risk score: %d."
                            .formatted(event.transactionId(), event.reason(), event.riskScore()))
                    .status(NotificationStatus.PENDING)
                    .build();

            notificationService.sendNotification(notification);
            acknowledgment.acknowledge();
            log.info("Created fraud notification for transactionId={}", event.transactionId());
        } catch (Exception exception) {
            log.error("Failed to process fraud notification message", exception);
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(topics = VERIFIED_TRANSACTIONS_TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void consumeVerifiedTransaction(String rawMessage, Acknowledgment acknowledgment) {
        try {
            TransactionEvent event = objectMapper.readValue(rawMessage, TransactionEvent.class);
            Notification notification = Notification.builder()
                    .transactionId(event.transactionId())
                    .accountId(event.accountId())
                    .type(NotificationType.TRANSACTION_APPROVED)
                    .message("Transaction %s was approved.".formatted(event.transactionId()))
                    .status(NotificationStatus.PENDING)
                    .build();

            notificationService.sendNotification(notification);
            acknowledgment.acknowledge();
            log.info("Created approval notification for transactionId={}", event.transactionId());
        } catch (Exception exception) {
            log.error("Failed to process verified transaction notification message", exception);
            acknowledgment.acknowledge();
        }
    }
}
