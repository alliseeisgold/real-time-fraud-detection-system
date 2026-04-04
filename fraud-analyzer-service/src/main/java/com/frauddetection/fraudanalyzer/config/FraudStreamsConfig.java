package com.frauddetection.fraudanalyzer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.fraudanalyzer.service.FrequencyTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FraudStreamsConfig {

    private static final String RAW_TRANSACTIONS_TOPIC = "transactions.raw";

    private final ObjectMapper objectMapper;
    private final FrequencyTracker frequencyTracker;

    @Value("${fraud.rules.frequency-window-seconds:60}")
    private long windowSeconds;

    @Value("${fraud.rules.frequency-max-count:3}")
    private long maxCount;

    @Bean
    public KStream<String, String> transactionFrequencyStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> rawTransactions = streamsBuilder.stream(
                RAW_TRANSACTIONS_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        rawTransactions
                .mapValues(this::readAccountId)
                .filter((key, accountId) -> accountId != null)
                .selectKey((key, accountId) -> accountId)
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(windowSeconds)))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("account-transaction-counts")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .toStream()
                .filter((accountWindow, count) -> count >= maxCount)
                .foreach((accountWindow, count) -> {
                    UUID accountId = UUID.fromString(accountWindow.key());
                    frequencyTracker.markFrequent(accountId);
                    log.info("Marked accountId={} as high frequency with count={}", accountId, count);
                });

        return rawTransactions;
    }

    private String readAccountId(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, TransactionEvent.class)
                    .accountId()
                    .toString();
        } catch (Exception exception) {
            log.warn("Kafka Streams could not parse transaction event for frequency analysis", exception);
            return null;
        }
    }
}
