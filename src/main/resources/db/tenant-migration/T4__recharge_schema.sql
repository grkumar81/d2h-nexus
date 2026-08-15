-- T4__recharge_schema.sql
-- Recharge transactions for a tenant schema.
-- No tenant_id column — the schema itself is the tenant boundary.

CREATE TABLE recharge_transactions (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    retailer_id         BIGINT          NOT NULL,
    asset_id            BIGINT,
    reference           VARCHAR(100)    NOT NULL,
    external_reference  VARCHAR(100),
    recharge_date       DATE            NOT NULL,
    amount              DECIMAL(15,2)   NOT NULL,
    recharge_type       VARCHAR(30)     NOT NULL DEFAULT 'REGULAR',
    recharge_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(30),
    payment_reference   VARCHAR(100),
    service_period      VARCHAR(100),
    description         VARCHAR(500),
    remarks             VARCHAR(500),
    source              VARCHAR(30)     NOT NULL DEFAULT 'MANUAL',
    reversed_by_id      BIGINT,
    reversal_of_id      BIGINT,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    CONSTRAINT pk_recharge_transactions PRIMARY KEY (id),
    CONSTRAINT uq_recharge_reference UNIQUE (reference),
    CONSTRAINT fk_recharge_retailer FOREIGN KEY (retailer_id) REFERENCES retailers  (id),
    CONSTRAINT fk_recharge_asset    FOREIGN KEY (asset_id)    REFERENCES stb_assets (id)
);

CREATE INDEX idx_recharge_retailer     ON recharge_transactions (retailer_id);
CREATE INDEX idx_recharge_date         ON recharge_transactions (recharge_date);
CREATE INDEX idx_recharge_status       ON recharge_transactions (recharge_status);
CREATE INDEX idx_recharge_type         ON recharge_transactions (recharge_type);
CREATE INDEX idx_recharge_external_ref ON recharge_transactions (external_reference);
