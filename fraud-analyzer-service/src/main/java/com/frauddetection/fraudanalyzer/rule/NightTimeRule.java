package com.frauddetection.fraudanalyzer.rule;

import com.frauddetection.common.FraudReason;
import com.frauddetection.common.TransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;

@Component
public class NightTimeRule implements FraudRule {

    private final int startHour;
    private final int endHour;
    private final BigDecimal amountThreshold;
    private final int NIGHT_TIME_RULE_WEIGHT = 10;

    public NightTimeRule(
            @Value("${fraud.rules.night-start-hour:0}") int startHour,
            @Value("${fraud.rules.night-end-hour:5}") int endHour,
            @Value("${fraud.rules.night-amount-threshold:3000}") BigDecimal amountThreshold
    ) {
        this.startHour = startHour;
        this.endHour = endHour;
        this.amountThreshold = amountThreshold;
    }

    @Override
    public boolean evaluate(TransactionEvent event) {
        int hour = event.timestamp().atZone(ZoneOffset.UTC).getHour();
        return hour >= startHour
                && hour <= endHour
                && event.amount().compareTo(amountThreshold) > 0;
    }

    @Override
    public FraudReason reason() {
        return FraudReason.NIGHT_TIME;
    }

    @Override
    public int weight() {
        return NIGHT_TIME_RULE_WEIGHT;
    }
}
