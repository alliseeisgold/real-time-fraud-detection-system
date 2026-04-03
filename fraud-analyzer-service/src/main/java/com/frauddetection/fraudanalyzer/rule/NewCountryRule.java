package com.frauddetection.fraudanalyzer.rule;

import com.frauddetection.common.FraudReason;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.fraudanalyzer.service.AccountCountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewCountryRule implements FraudRule {

    private final AccountCountryService accountCountryService;
    private final int NEW_COUNTRY_RULE_WEIGHT = 20;

    @Override
    public boolean evaluate(TransactionEvent event) {
        return accountCountryService.isNewCountry(event);
    }

    @Override
    public FraudReason reason() {
        return FraudReason.NEW_COUNTRY;
    }

    @Override
    public int weight() {
        return NEW_COUNTRY_RULE_WEIGHT;
    }
}
