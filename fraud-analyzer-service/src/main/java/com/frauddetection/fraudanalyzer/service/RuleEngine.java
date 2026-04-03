package com.frauddetection.fraudanalyzer.service;

import com.frauddetection.common.FraudReason;
import com.frauddetection.common.TransactionEvent;
import com.frauddetection.fraudanalyzer.dto.AnalysisResult;
import com.frauddetection.fraudanalyzer.rule.FraudRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleEngine {

    private final List<FraudRule> fraudRules;

    public AnalysisResult analyze(TransactionEvent event) {
        List<FraudRule> matchedRules = fraudRules.stream()
                .filter(rule -> rule.evaluate(event))
                .toList();

        List<FraudReason> reasons = matchedRules.stream()
                .map(FraudRule::reason)
                .toList();

        int riskScore = matchedRules.stream()
                .mapToInt(FraudRule::weight)
                .sum();

        return new AnalysisResult(!reasons.isEmpty(), reasons, riskScore);
    }
}
