CREATE TABLE fraud_alerts (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    reason TEXT NOT NULL,
    risk_score INTEGER NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_fraud_alerts_account_id ON fraud_alerts (account_id);
CREATE INDEX idx_fraud_alerts_detected_at ON fraud_alerts (detected_at DESC);
