package com.frauddetection.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.Currency;
import com.frauddetection.common.TransactionStatus;
import com.frauddetection.payment.dto.CreateTransactionRequest;
import com.frauddetection.payment.dto.TransactionResponse;
import com.frauddetection.payment.entity.OutboxEvent;
import com.frauddetection.payment.entity.OutboxEventStatus;
import com.frauddetection.payment.kafka.OutboxPublisher;
import com.frauddetection.payment.repository.OutboxEventRepository;
import com.frauddetection.payment.repository.TransactionRepository;
import com.frauddetection.payment.service.TransactionService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "payment.outbox.initial-delay-ms=3600000",
        "payment.outbox.publish-timeout-seconds=5"
})
class OutboxPatternIntegrationTest {

    private static final String RAW_TRANSACTIONS_TOPIC = "transactions.raw";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16"))
            .withDatabaseName("payment_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void createTransactionPersistsTransactionAndPublishesOutboxEventToKafka() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                UUID.randomUUID(),
                new BigDecimal("150.75"),
                Currency.USD,
                "US",
                "RETAIL"
        );

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(transactionRepository.findById(response.id()))
                .isPresent()
                .get()
                .satisfies(transaction -> {
                    assertThat(transaction.getAccountId()).isEqualTo(request.accountId());
                    assertThat(transaction.getAmount()).isEqualByComparingTo(request.amount());
                    assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
                });

        OutboxEvent pendingEvent = outboxEventRepository.findByAggregateId(response.id()).orElseThrow();
        assertThat(pendingEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(pendingEvent.getTopic()).isEqualTo(RAW_TRANSACTIONS_TOPIC);
        assertThat(pendingEvent.getRetryCount()).isZero();
        assertThat(pendingEvent.getPublishedAt()).isNull();

        outboxPublisher.publishPendingEvents();

        OutboxEvent publishedEvent = outboxEventRepository.findByAggregateId(response.id()).orElseThrow();
        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();
        assertThat(publishedEvent.getLastError()).isNull();

        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of(RAW_TRANSACTIONS_TOPIC));

            ConsumerRecord<String, String> record = pollForTransaction(consumer, response.id());
            assertThat(record.key()).isEqualTo(response.id().toString());

            JsonNode payload = objectMapper.readTree(record.value());
            assertThat(payload.get("transactionId").asText()).isEqualTo(response.id().toString());
            assertThat(payload.get("accountId").asText()).isEqualTo(request.accountId().toString());
            assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo(request.amount());
            assertThat(payload.get("currency").asText()).isEqualTo(request.currency().name());
            assertThat(payload.get("country").asText()).isEqualTo(request.country());
            assertThat(payload.get("merchantCategory").asText()).isEqualTo(request.merchantCategory());
            assertThat(payload.get("timestamp").asText()).isNotBlank();
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-outbox-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private ConsumerRecord<String, String> pollForTransaction(KafkaConsumer<String, String> consumer, UUID transactionId) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();

        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records.records(RAW_TRANSACTIONS_TOPIC)) {
                if (transactionId.toString().equals(record.key())) {
                    return record;
                }
            }
        }

        throw new AssertionError("No Kafka record found for transactionId=" + transactionId);
    }
}
