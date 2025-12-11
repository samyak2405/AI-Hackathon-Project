CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    uuid VARCHAR(255),
    service_id VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_transaction_id ON transactions(transaction_id);

