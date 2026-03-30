package com.frauddetection.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 128)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = OutboxEventStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
