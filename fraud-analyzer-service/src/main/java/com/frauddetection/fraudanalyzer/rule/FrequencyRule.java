package com.frauddetection.fraudanalyzer.rule;

import com.frauddetection.common.FraudReason;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.fraudanalyzer.service.FrequencyTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FrequencyRule implements FraudRule {

    private final int FREQUENCY_RULE_WEIGHT = 30;
    private final FrequencyTracker frequencyTracker;

    @Override
    public boolean evaluate(TransactionEvent event) {
        return frequencyTracker.isFrequent(event.accountId());
    }

    @Override
    public FraudReason reason() {
        return FraudReason.HIGH_FREQUENCY;
    }

    @Override
    public int weight() {
        return FREQUENCY_RULE_WEIGHT;
    }
}
