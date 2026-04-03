package com.frauddetection.fraudanalyzer.repository;

import com.frauddetection.fraudanalyzer.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
