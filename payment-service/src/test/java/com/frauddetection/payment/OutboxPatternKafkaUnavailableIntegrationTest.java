package com.frauddetection.payment;

import com.frauddetection.common.Currency;
import com.frauddetection.payment.dto.CreateTransactionRequest;
import com.frauddetection.payment.dto.TransactionResponse;
import com.frauddetection.payment.entity.OutboxEvent;
import com.frauddetection.payment.entity.OutboxEventStatus;
import com.frauddetection.payment.kafka.OutboxPublisher;
import com.frauddetection.payment.repository.OutboxEventRepository;
import com.frauddetection.payment.repository.TransactionRepository;
import com.frauddetection.payment.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "payment.outbox.initial-delay-ms=3600000",
        "payment.outbox.publish-timeout-seconds=2",
        "spring.kafka.bootstrap-servers=127.0.0.1:65534",
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.admin.fail-fast=false",
        "spring.kafka.admin.properties.default.api.timeout.ms=500",
        "spring.kafka.admin.properties.request.timeout.ms=500",
        "spring.kafka.admin.properties.retries=0",
        "spring.kafka.producer.properties.max.block.ms=500",
        "spring.kafka.producer.properties.request.timeout.ms=500",
        "spring.kafka.producer.properties.delivery.timeout.ms=1000"
})
class OutboxPatternKafkaUnavailableIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16"))
            .withDatabaseName("payment_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void publisherKeepsEventPendingAndRetriesWhenKafkaIsUnavailable() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                UUID.randomUUID(),
                new BigDecimal("90.50"),
                Currency.EUR,
                "DE",
                "TRAVEL"
        );

        TransactionResponse response = transactionService.createTransaction(request);
        OutboxEvent createdEvent = outboxEventRepository.findByAggregateId(response.id()).orElseThrow();

        assertThat(createdEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(createdEvent.getRetryCount()).isZero();

        outboxPublisher.publishPendingEvents();

        OutboxEvent firstRetry = outboxEventRepository.findByAggregateId(response.id()).orElseThrow();
        assertThat(firstRetry.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(firstRetry.getRetryCount()).isEqualTo(1);
        assertThat(firstRetry.getPublishedAt()).isNull();
        assertThat(firstRetry.getLastError()).isNotBlank();

        outboxPublisher.publishPendingEvents();

        OutboxEvent secondRetry = outboxEventRepository.findByAggregateId(response.id()).orElseThrow();
        assertThat(secondRetry.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(secondRetry.getRetryCount()).isEqualTo(2);
        assertThat(secondRetry.getPublishedAt()).isNull();
        assertThat(secondRetry.getLastError()).isNotBlank();
    }
}
