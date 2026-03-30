package com.frauddetection.payment.kafka;

import com.frauddetection.payment.entity.OutboxEvent;
import com.frauddetection.payment.entity.OutboxEventStatus;
import com.frauddetection.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicBoolean publishing = new AtomicBoolean(false);

    @Value("${payment.outbox.max-retries:10}")
    private int maxRetries;

    @Value("${payment.outbox.publish-timeout-seconds:10}")
    private long publishTimeoutSeconds;

    @Scheduled(
            fixedDelayString = "${payment.outbox.poll-delay-ms:5000}",
            initialDelayString = "${payment.outbox.initial-delay-ms:0}"
    )
    public void publishPendingEvents() {
        if (!publishing.compareAndSet(false, true)) {
            log.debug("Skipping outbox publishing because the previous run is still active");
            return;
        }

        try {
            List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);
            if (events.isEmpty()) {
                return;
            }

            log.info("Publishing {} pending outbox event(s)", events.size());
            events.forEach(this::publishEvent);
        } finally {
            publishing.set(false);
        }
    }

    private void publishEvent(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                    .get(publishTimeoutSeconds, TimeUnit.SECONDS);

            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            outboxEventRepository.save(event);

            log.info("Published outbox event id={} topic={} aggregateId={}",
                    event.getId(), event.getTopic(), event.getAggregateId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            markPublishFailure(event, exception);
        } catch (Exception exception) {
            markPublishFailure(event, exception);
        }
    }

    private void markPublishFailure(OutboxEvent event, Exception exception) {
        int nextRetryCount = event.getRetryCount() + 1;
        event.setRetryCount(nextRetryCount);
        event.setLastError(exception.getMessage());

        if (nextRetryCount >= maxRetries) {
            event.setStatus(OutboxEventStatus.FAILED);
        }

        outboxEventRepository.save(event);
        log.warn("Failed to publish outbox event id={} retryCount={} status={}",
                event.getId(), nextRetryCount, event.getStatus(), exception);
    }
}
