package com.frauddetection.fraudanalyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FrequencyTracker {

    private final Map<UUID, Instant> frequentAccounts = new ConcurrentHashMap<>();
    private final Duration retention;

    public FrequencyTracker(@Value("${fraud.rules.frequency-window-seconds:60}") long windowSeconds) {
        this.retention = Duration.ofSeconds(windowSeconds);
    }

    public void markFrequent(UUID accountId) {
        frequentAccounts.put(accountId, Instant.now());
    }

    public boolean isFrequent(UUID accountId) {
        Instant markedAt = frequentAccounts.get(accountId);
        if (markedAt == null) {
            return false;
        }

        if (markedAt.plus(retention).isBefore(Instant.now())) {
            frequentAccounts.remove(accountId);
            return false;
        }

        return true;
    }
}
