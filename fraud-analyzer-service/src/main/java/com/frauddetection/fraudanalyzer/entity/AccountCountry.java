package com.frauddetection.fraudanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCountry {

    @Id
    private UUID accountId;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onChange() {
        updatedAt = Instant.now();
    }
}
