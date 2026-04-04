package com.frauddetection.fraudanalyzer.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.FraudAlertEvent;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.fraudanalyzer.dto.AnalysisResult;
import com.frauddetection.fraudanalyzer.entity.FraudAlert;
import com.frauddetection.fraudanalyzer.service.AccountCountryService;
import com.frauddetection.fraudanalyzer.service.FraudAlertService;
import com.frauddetection.fraudanalyzer.service.ProcessedEventService;
import com.frauddetection.fraudanalyzer.service.RuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FraudAnalyzerConsumer {

    private static final String RAW_TRANSACTIONS_TOPIC = "transactions.raw";
    private static final String VERIFIED_TRANSACTIONS_TOPIC = "transactions.verified";
    private static final String FRAUD_TRANSACTIONS_TOPIC = "transactions.fraud";
    private static final String DEAD_LETTER_TOPIC = "transactions.dead-letter";

    private final ObjectMapper objectMapper;
    private final RuleEngine ruleEngine;
    private final FraudAlertService fraudAlertService;
    private final ProcessedEventService processedEventService;
    private final AccountCountryService accountCountryService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final long publishTimeoutSeconds;

    public FraudAnalyzerConsumer(
            ObjectMapper objectMapper,
            RuleEngine ruleEngine,
            FraudAlertService fraudAlertService,
            ProcessedEventService processedEventService,
            AccountCountryService accountCountryService,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${fraud.kafka.publish-timeout-seconds:10}") long publishTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.ruleEngine = ruleEngine;
        this.fraudAlertService = fraudAlertService;
        this.processedEventService = processedEventService;
        this.accountCountryService = accountCountryService;
        this.kafkaTemplate = kafkaTemplate;
        this.publishTimeoutSeconds = publishTimeoutSeconds;
    }

    @KafkaListener(topics = RAW_TRANSACTIONS_TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void consume(String rawMessage, Acknowledgment acknowledgment) throws Exception {
        try {
            TransactionEvent event = objectMapper.readValue(rawMessage, TransactionEvent.class);

            if (processedEventService.isProcessed(event.transactionId())) {
                log.info("Skipping duplicate transactionId={}", event.transactionId());
                acknowledgment.acknowledge();
                return;
            }

            AnalysisResult result = ruleEngine.analyze(event);
            if (result.fraud()) {
                publishFraudAlert(event, result);
            } else {
                publishVerifiedTransaction(event);
            }

            accountCountryService.recordCountry(event);
            processedEventService.markProcessed(event.transactionId());
            acknowledgment.acknowledge();

            log.info("Processed transactionId={} fraud={} riskScore={}",
                    event.transactionId(), result.fraud(), result.riskScore());
        } catch (Exception exception) {
            log.error("Failed to process raw transaction message", exception);
            publishDeadLetter(rawMessage);
            acknowledgment.acknowledge();
        }
    }

    private void publishFraudAlert(TransactionEvent event, AnalysisResult result) throws Exception {
        FraudAlert fraudAlert = fraudAlertService.saveAlert(event, result);
        FraudAlertEvent fraudAlertEvent = new FraudAlertEvent(
                fraudAlert.getTransactionId(),
                fraudAlert.getAccountId(),
                fraudAlert.getReason(),
                fraudAlert.getRiskScore(),
                fraudAlert.getDetectedAt() == null ? Instant.now() : fraudAlert.getDetectedAt()
        );

        publish(FRAUD_TRANSACTIONS_TOPIC, event.transactionId().toString(), fraudAlertEvent);
    }

    private void publishVerifiedTransaction(TransactionEvent event) throws Exception {
        publish(VERIFIED_TRANSACTIONS_TOPIC, event.transactionId().toString(), event);
    }

    private void publishDeadLetter(String rawMessage) throws Exception {
        kafkaTemplate.send(DEAD_LETTER_TOPIC, resolveDeadLetterKey(rawMessage), rawMessage)
                .get(publishTimeoutSeconds, TimeUnit.SECONDS);
    }

    private void publish(String topic, String key, Object event) throws Exception {
        kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event))
                .get(publishTimeoutSeconds, TimeUnit.SECONDS);
    }

    private String resolveDeadLetterKey(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            JsonNode transactionId = root.get("transactionId");
            if (transactionId != null && !transactionId.isNull()) {
                return transactionId.asText();
            }
        } catch (Exception ignored) {
            log.debug("Could not extract transactionId for dead-letter key");
        }

        return "unknown";
    }
}
