-- T3__finance_schema.sql
-- Financial transactions and outbox events for a tenant schema.
-- No tenant_id column — the schema itself is the tenant boundary.

CREATE TABLE financial_transactions (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    retailer_id         BIGINT          NOT NULL,
    transaction_type    VARCHAR(30)     NOT NULL,
    transaction_status  VARCHAR(20)     NOT NULL DEFAULT 'POSTED',
    transaction_date    DATE            NOT NULL,
    amount              DECIMAL(15,2)   NOT NULL,
    payment_method      VARCHAR(30),
    reference           VARCHAR(100),
    payment_reference   VARCHAR(100),
    description         VARCHAR(500),
    remarks             VARCHAR(500),
    source              VARCHAR(30)     NOT NULL DEFAULT 'MANUAL',
    sale_id             BIGINT,
    reversed_by_id      BIGINT,
    reversal_of_id      BIGINT,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    CONSTRAINT pk_financial_transactions PRIMARY KEY (id),
    CONSTRAINT uq_finance_reference UNIQUE (reference),
    CONSTRAINT fk_finance_retailer FOREIGN KEY (retailer_id) REFERENCES retailers (id),
    CONSTRAINT fk_finance_sale     FOREIGN KEY (sale_id)     REFERENCES stb_sales (id)
);

CREATE INDEX idx_finance_retailer ON financial_transactions (retailer_id);
CREATE INDEX idx_finance_type     ON financial_transactions (transaction_type);
CREATE INDEX idx_finance_status   ON financial_transactions (transaction_status);
CREATE INDEX idx_finance_date     ON financial_transactions (transaction_date);
CREATE INDEX idx_finance_sale     ON financial_transactions (sale_id);

CREATE TABLE outbox_events (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    event_type   VARCHAR(100)  NOT NULL,
    aggregate_id VARCHAR(100)  NOT NULL,
    payload      TEXT          NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    attempts     INT           NOT NULL DEFAULT 0,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    next_retry_at DATETIME(6),
    error_message VARCHAR(1000),
    updated_at   DATETIME(6),
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status  ON outbox_events (status, created_at);
CREATE INDEX idx_outbox_pending ON outbox_events (status, next_retry_at);
