package com.frauddetection.fraudanalyzer.rule;

import com.frauddetection.common.FraudReason;
import com.frauddetection.common.TransactionEvent;

public interface FraudRule {

    boolean evaluate(TransactionEvent event);

    FraudReason reason();

    int weight();
}
