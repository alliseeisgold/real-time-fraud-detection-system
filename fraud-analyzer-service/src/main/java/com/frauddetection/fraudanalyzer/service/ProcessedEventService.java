package com.frauddetection.fraudanalyzer.service;

import com.frauddetection.fraudanalyzer.entity.ProcessedEvent;
import com.frauddetection.fraudanalyzer.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public boolean isProcessed(UUID transactionId) {
        return processedEventRepository.existsById(transactionId);
    }

    @Transactional
    public void markProcessed(UUID transactionId) {
        processedEventRepository.save(ProcessedEvent.builder()
                .transactionId(transactionId)
                .build());
    }
}
