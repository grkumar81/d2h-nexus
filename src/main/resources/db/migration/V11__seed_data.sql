-- V11__seed_data.sql
-- Seeds default users and a demo tenant for real-world testing.
-- Passwords are BCrypt-encoded: Admin@1234

-- ── 1. PLATFORM_ADMIN user ────────────────────────────────────────────────────
INSERT INTO users (username, email, password_hash, full_name, phone, status,
                   failed_login_count, created_by)
VALUES ('platform_admin',
        'platform@d2h.local',
        '$2a$10$GkCb296KuITXYFiadX/poegX0Qfd7GJgw8B6S7tBCaOG21BqyR9.u',
        'Platform Administrator',
        NULL,
        'ACTIVE',
        0,
        'system')
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO user_roles (user_id, role_name)
SELECT id, 'PLATFORM_ADMIN'
FROM   users
WHERE  username = 'platform_admin'
ON DUPLICATE KEY UPDATE role_name = role_name;

-- ── 2. DEMO tenant (platform schema row) ─────────────────────────────────────
INSERT INTO tenants (tenant_code, name, schema_name, email, phone,
                     status, subscription_expiry, grace_period_days, created_by)
VALUES ('DEMO',
        'Demo Distributor',
        'd2h_tenant_demo',
        'demo@d2h.local',
        '+91-9000000001',
        'ACTIVE',
        DATE_ADD(CURDATE(), INTERVAL 1 YEAR),
        30,
        'system')
ON DUPLICATE KEY UPDATE tenant_code = tenant_code;

-- ── 3. Provision d2h_tenant_demo schema ──────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS d2h_tenant_demo
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- T1 — retailers
CREATE TABLE IF NOT EXISTS d2h_tenant_demo.retailers (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    retailer_code    VARCHAR(50)     NOT NULL,
    retailer_name    VARCHAR(255)    NOT NULL,
    mobile           VARCHAR(20)     NOT NULL,
    alternate_mobile VARCHAR(20),
    email            VARCHAR(255),
    address          TEXT,
    city             VARCHAR(100),
    state            VARCHAR(100),
    pin_code         VARCHAR(10),
    gst_number       VARCHAR(20),
    pan_number       VARCHAR(20),
    status           VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    joining_date     DATE,
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT pk_retailers PRIMARY KEY (id),
    CONSTRAINT uq_retailer_code UNIQUE (retailer_code)
);

CREATE INDEX idx_retailers_status ON d2h_tenant_demo.retailers (status);
CREATE INDEX idx_retailers_mobile ON d2h_tenant_demo.retailers (mobile);
CREATE INDEX idx_retailers_name   ON d2h_tenant_demo.retailers (retailer_name);

CREATE TABLE IF NOT EXISTS d2h_tenant_demo.file_uploads (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    upload_type       VARCHAR(50)     NOT NULL,
    file_name         VARCHAR(255)    NOT NULL,
    file_size         BIGINT,
    status            VARCHAR(30)     NOT NULL DEFAULT 'PROCESSING',
    total_records     INT             NOT NULL DEFAULT 0,
    success_records   INT             NOT NULL DEFAULT 0,
    failed_records    INT             NOT NULL DEFAULT 0,
    duplicate_records INT             NOT NULL DEFAULT 0,
    error_file_path   VARCHAR(500),
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by        VARCHAR(100),
    CONSTRAINT pk_file_uploads PRIMARY KEY (id)
);

CREATE INDEX idx_file_uploads_type ON d2h_tenant_demo.file_uploads (upload_type);

CREATE TABLE IF NOT EXISTS d2h_tenant_demo.file_upload_errors (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    upload_id     BIGINT NOT NULL,
    `row_number`   INT    NOT NULL,
    row_data      TEXT,
    error_message TEXT   NOT NULL,
    CONSTRAINT pk_file_upload_errors PRIMARY KEY (id),
    CONSTRAINT fk_upload_errors_upload FOREIGN KEY (upload_id) REFERENCES d2h_tenant_demo.file_uploads (id)
);

CREATE INDEX idx_upload_errors_upload ON d2h_tenant_demo.file_upload_errors (upload_id);

-- T2 — assets
CREATE TABLE IF NOT EXISTS d2h_tenant_demo.stb_assets (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    serial_number    VARCHAR(100)    NOT NULL,
    box_number       VARCHAR(100),
    model            VARCHAR(100),
    manufacturer     VARCHAR(100),
    batch            VARCHAR(100),
    purchase_date    DATE,
    purchase_cost    DECIMAL(15,2),
    status           VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    retailer_id      BIGINT,
    tagging_date     DATE,
    sale_date        DATE,
    activation_date  DATE,
    return_date      DATE,
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_stb_assets PRIMARY KEY (id),
    CONSTRAINT uq_asset_serial UNIQUE (serial_number),
    CONSTRAINT fk_assets_retailer FOREIGN KEY (retailer_id) REFERENCES d2h_tenant_demo.retailers (id)
);

CREATE INDEX idx_assets_status   ON d2h_tenant_demo.stb_assets (status);
CREATE INDEX idx_assets_retailer ON d2h_tenant_demo.stb_assets (retailer_id);
CREATE INDEX idx_assets_serial   ON d2h_tenant_demo.stb_assets (serial_number);

CREATE TABLE IF NOT EXISTS d2h_tenant_demo.stb_asset_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    asset_id    BIGINT       NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20)  NOT NULL,
    retailer_id BIGINT,
    changed_by  VARCHAR(100),
    remarks     VARCHAR(500),
    changed_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_stb_asset_history PRIMARY KEY (id),
    CONSTRAINT fk_history_asset FOREIGN KEY (asset_id) REFERENCES d2h_tenant_demo.stb_assets (id)
);

CREATE INDEX idx_asset_history_asset ON d2h_tenant_demo.stb_asset_history (asset_id);
CREATE INDEX idx_asset_history_date  ON d2h_tenant_demo.stb_asset_history (changed_at);

CREATE TABLE IF NOT EXISTS d2h_tenant_demo.stb_sales (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    retailer_id      BIGINT          NOT NULL,
    transaction_date DATE            NOT NULL,
    total_amount     DECIMAL(15,2)   NOT NULL,
    payment_status   VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    reference        VARCHAR(100),
    remarks          VARCHAR(500),
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    CONSTRAINT pk_stb_sales PRIMARY KEY (id),
    CONSTRAINT fk_sales_retailer FOREIGN KEY (retailer_id) REFERENCES d2h_tenant_demo.retailers (id)
);

CREATE INDEX idx_sales_retailer ON d2h_tenant_demo.stb_sales (retailer_id);
CREATE INDEX idx_sales_date     ON d2h_tenant_demo.stb_sales (transaction_date);

CREATE TABLE IF NOT EXISTS d2h_tenant_demo.stb_sale_items (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    sale_id    BIGINT        NOT NULL,
    asset_id   BIGINT        NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    CONSTRAINT pk_stb_sale_items PRIMARY KEY (id),
    CONSTRAINT uq_sale_item_asset UNIQUE (sale_id, asset_id),
    CONSTRAINT fk_sale_items_sale  FOREIGN KEY (sale_id)  REFERENCES d2h_tenant_demo.stb_sales  (id),
    CONSTRAINT fk_sale_items_asset FOREIGN KEY (asset_id) REFERENCES d2h_tenant_demo.stb_assets (id)
);

CREATE INDEX idx_sale_items_sale  ON d2h_tenant_demo.stb_sale_items (sale_id);
CREATE INDEX idx_sale_items_asset ON d2h_tenant_demo.stb_sale_items (asset_id);

-- T3 — finance
CREATE TABLE IF NOT EXISTS d2h_tenant_demo.financial_transactions (
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
    CONSTRAINT fk_finance_retailer FOREIGN KEY (retailer_id) REFERENCES d2h_tenant_demo.retailers (id),
    CONSTRAINT fk_finance_sale     FOREIGN KEY (sale_id)     REFERENCES d2h_tenant_demo.stb_sales (id)
);

CREATE INDEX idx_finance_retailer ON d2h_tenant_demo.financial_transactions (retailer_id);
CREATE INDEX idx_finance_type     ON d2h_tenant_demo.financial_transactions (transaction_type);
CREATE INDEX idx_finance_status   ON d2h_tenant_demo.financial_transactions (transaction_status);
CREATE INDEX idx_finance_date     ON d2h_tenant_demo.financial_transactions (transaction_date);
CREATE INDEX idx_finance_sale     ON d2h_tenant_demo.financial_transactions (sale_id);

CREATE TABLE IF NOT EXISTS d2h_tenant_demo.outbox_events (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    event_type    VARCHAR(100)  NOT NULL,
    aggregate_id  VARCHAR(100)  NOT NULL,
    payload       TEXT          NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    attempts      INT           NOT NULL DEFAULT 0,
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at  DATETIME(6),
    next_retry_at DATETIME(6),
    error_message VARCHAR(1000),
    updated_at    DATETIME(6),
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status  ON d2h_tenant_demo.outbox_events (status, created_at);
CREATE INDEX idx_outbox_pending ON d2h_tenant_demo.outbox_events (status, next_retry_at);

-- T4 — recharge
CREATE TABLE IF NOT EXISTS d2h_tenant_demo.recharge_transactions (
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
    CONSTRAINT fk_recharge_retailer FOREIGN KEY (retailer_id) REFERENCES d2h_tenant_demo.retailers  (id),
    CONSTRAINT fk_recharge_asset    FOREIGN KEY (asset_id)    REFERENCES d2h_tenant_demo.stb_assets (id)
);

CREATE INDEX idx_recharge_retailer     ON d2h_tenant_demo.recharge_transactions (retailer_id);
CREATE INDEX idx_recharge_date         ON d2h_tenant_demo.recharge_transactions (recharge_date);
CREATE INDEX idx_recharge_status       ON d2h_tenant_demo.recharge_transactions (recharge_status);
CREATE INDEX idx_recharge_type         ON d2h_tenant_demo.recharge_transactions (recharge_type);
CREATE INDEX idx_recharge_external_ref ON d2h_tenant_demo.recharge_transactions (external_reference);

-- T5 — notifications
CREATE TABLE IF NOT EXISTS d2h_tenant_demo.notification_config (
    id         BIGINT          NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(100)    NOT NULL,
    channel    VARCHAR(20)     NOT NULL,
    enabled    TINYINT(1)      NOT NULL DEFAULT 1,
    recipients VARCHAR(2000),
    created_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100),
    CONSTRAINT pk_notification_config PRIMARY KEY (id),
    CONSTRAINT uq_notif_config_event_channel UNIQUE (event_type, channel)
);

CREATE TABLE IF NOT EXISTS d2h_tenant_demo.notification_delivery (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    outbox_event_id BIGINT          NOT NULL,
    channel         VARCHAR(20)     NOT NULL,
    recipient       VARCHAR(500)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    attempts        INT             NOT NULL DEFAULT 0,
    sent_at         DATETIME(6),
    error_message   VARCHAR(1000),
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_notification_delivery PRIMARY KEY (id),
    CONSTRAINT fk_notif_delivery_outbox FOREIGN KEY (outbox_event_id) REFERENCES d2h_tenant_demo.outbox_events (id)
);

CREATE INDEX idx_notif_delivery_status ON d2h_tenant_demo.notification_delivery (status);
CREATE INDEX idx_notif_delivery_outbox ON d2h_tenant_demo.notification_delivery (outbox_event_id);

-- T6 — audit
CREATE TABLE IF NOT EXISTS d2h_tenant_demo.audit_logs (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    entity_type  VARCHAR(100)    NOT NULL,
    entity_id    VARCHAR(100)    NOT NULL,
    action       VARCHAR(50)     NOT NULL,
    performed_by VARCHAR(100)    NOT NULL,
    details      TEXT,
    ip_address   VARCHAR(45),
    created_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_entity ON d2h_tenant_demo.audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_date   ON d2h_tenant_demo.audit_logs (created_at);
CREATE INDEX idx_audit_user   ON d2h_tenant_demo.audit_logs (performed_by);

-- ── 4. TENANT_ADMIN user for DEMO tenant ─────────────────────────────────────
INSERT INTO users (username, email, password_hash, full_name, phone, status,
                   failed_login_count, created_by)
VALUES ('demo_admin',
        'admin@demo.local',
        '$2a$10$GkCb296KuITXYFiadX/poegX0Qfd7GJgw8B6S7tBCaOG21BqyR9.u',
        'Demo Admin',
        '+91-9000000002',
        'ACTIVE',
        0,
        'system')
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO user_roles (user_id, role_name)
SELECT id, 'TENANT_ADMIN'
FROM   users
WHERE  username = 'demo_admin'
ON DUPLICATE KEY UPDATE role_name = role_name;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT u.id, t.id
FROM   users u, tenants t
WHERE  u.username = 'demo_admin'
  AND  t.tenant_code = 'DEMO'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- ── 5. Sample retailers in demo tenant ───────────────────────────────────────
INSERT INTO d2h_tenant_demo.retailers
    (retailer_code, retailer_name, mobile, email, city, state, status, joining_date, created_by)
VALUES
    ('RET001', 'Sharma Electronics',   '9811000001', 'sharma@demo.local',  'Delhi',   'Delhi',     'ACTIVE', CURDATE(), 'system'),
    ('RET002', 'Kumar TV Centre',      '9811000002', 'kumar@demo.local',   'Mumbai',  'Maharashtra','ACTIVE', CURDATE(), 'system'),
    ('RET003', 'Patel Digital Store',  '9811000003', 'patel@demo.local',   'Ahmedabad','Gujarat',  'ACTIVE', CURDATE(), 'system');
