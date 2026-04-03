package com.frauddetection.fraudanalyzer.rule;

import com.frauddetection.common.FraudReason;
import com.frauddetection.common.TransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighAmountRule implements FraudRule {

    private final BigDecimal threshold;
    private final int HIGH_AMOUNT_RULE_WEIGHT = 40;

    public HighAmountRule(@Value("${fraud.rules.high-amount-threshold:10000}") BigDecimal threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean evaluate(TransactionEvent event) {
        return event.amount().compareTo(threshold) > 0;
    }

    @Override
    public FraudReason reason() {
        return FraudReason.HIGH_AMOUNT;
    }

    @Override
    public int weight() {
        return HIGH_AMOUNT_RULE_WEIGHT;
    }
}
