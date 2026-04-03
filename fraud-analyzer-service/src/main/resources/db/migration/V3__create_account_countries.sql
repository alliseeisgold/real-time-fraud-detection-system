CREATE TABLE account_countries (
    account_id UUID PRIMARY KEY,
    country VARCHAR(2) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
