package com.frauddetection.fraudanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private int riskScore;

    @Column(nullable = false)
    private Instant detectedAt;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}
