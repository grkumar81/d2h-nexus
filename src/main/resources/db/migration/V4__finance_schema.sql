-- V4__finance_schema.sql
-- Financial transactions, adjustments, outbox events.
-- Tenant isolation via tenant_id. Duplicate prevention via unique reference constraint.
-- Compatible with MySQL 8.x and PostgreSQL (future migration).

CREATE TABLE financial_transactions (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id           BIGINT          NOT NULL,
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
    CONSTRAINT uq_finance_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT fk_finance_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants   (id),
    CONSTRAINT fk_finance_retailer FOREIGN KEY (retailer_id) REFERENCES retailers (id),
    CONSTRAINT fk_finance_sale     FOREIGN KEY (sale_id)     REFERENCES stb_sales (id)
);

CREATE INDEX idx_finance_tenant_retailer ON financial_transactions (tenant_id, retailer_id);
CREATE INDEX idx_finance_tenant_type     ON financial_transactions (tenant_id, transaction_type);
CREATE INDEX idx_finance_tenant_status   ON financial_transactions (tenant_id, transaction_status);
CREATE INDEX idx_finance_tenant_date     ON financial_transactions (tenant_id, transaction_date);
CREATE INDEX idx_finance_sale            ON financial_transactions (sale_id);

-- Outbox events stub for future email/WhatsApp notification phase
CREATE TABLE outbox_events (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id    BIGINT        NOT NULL,
    event_type   VARCHAR(100)  NOT NULL,
    aggregate_id VARCHAR(100)  NOT NULL,
    payload      TEXT          NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    attempts     INT           NOT NULL DEFAULT 0,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT fk_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX idx_outbox_status ON outbox_events (status, created_at);
