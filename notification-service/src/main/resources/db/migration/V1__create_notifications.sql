CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT uq_notifications_transaction_type UNIQUE (transaction_id, type)
);

CREATE INDEX idx_notifications_account_id ON notifications (account_id);
CREATE INDEX idx_notifications_sent_at ON notifications (sent_at DESC);
