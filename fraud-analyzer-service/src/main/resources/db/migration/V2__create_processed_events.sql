CREATE TABLE processed_events (
    transaction_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
