CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    country VARCHAR(2) NOT NULL,
    merchant_category VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transactions_account_id_created_at
    ON transactions (account_id, created_at DESC);
