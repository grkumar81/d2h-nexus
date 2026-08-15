-- D2H Platform — full setup script
-- Only CREATE and INSERT statements. Safe to run multiple times.

CREATE DATABASE IF NOT EXISTS d2h_platform
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'd2h_user'@'%' IDENTIFIED BY 'd2h_pass';
GRANT ALL PRIVILEGES ON d2h_platform.* TO 'd2h_user'@'%';
FLUSH PRIVILEGES;

USE d2h_platform;

-- ── V1: Platform schema ───────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS tenants (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_code   VARCHAR(50)     NOT NULL,
    name          VARCHAR(255)    NOT NULL,
    status        VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by    VARCHAR(100),
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uq_tenants_code UNIQUE (tenant_code)
);

CREATE TABLE IF NOT EXISTS roles (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    name  VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS users (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    username             VARCHAR(100)  NOT NULL,
    email                VARCHAR(255)  NOT NULL,
    password_hash        VARCHAR(255)  NOT NULL,
    full_name            VARCHAR(255),
    status               VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    failed_login_count   INT           NOT NULL DEFAULT 0,
    locked_until         DATETIME(6),
    created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by           VARCHAR(100),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id    BIGINT      NOT NULL,
    role_name  VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_name),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS user_tenants (
    user_id    BIGINT  NOT NULL,
    tenant_id  BIGINT  NOT NULL,
    CONSTRAINT pk_user_tenants PRIMARY KEY (user_id, tenant_id),
    CONSTRAINT fk_user_tenants_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_user_tenants_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS tenant_configurations (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id    BIGINT        NOT NULL,
    config_key   VARCHAR(100)  NOT NULL,
    config_value TEXT,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_tenant_configurations PRIMARY KEY (id),
    CONSTRAINT uq_tenant_config UNIQUE (tenant_id, config_key),
    CONSTRAINT fk_tenant_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

INSERT IGNORE INTO roles (name) VALUES ('TENANT_ADMIN');
INSERT IGNORE INTO roles (name) VALUES ('FINANCE_USER');
INSERT IGNORE INTO roles (name) VALUES ('OPERATIONS_USER');
INSERT IGNORE INTO roles (name) VALUES ('READ_ONLY');

-- ── V2: Retailer schema ───────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS retailers (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT          NOT NULL,
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
    CONSTRAINT uq_retailer_code_tenant UNIQUE (tenant_id, retailer_code),
    CONSTRAINT fk_retailers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS file_uploads (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT          NOT NULL,
    upload_type      VARCHAR(50)     NOT NULL,
    file_name        VARCHAR(255)    NOT NULL,
    file_size        BIGINT,
    status           VARCHAR(30)     NOT NULL DEFAULT 'PROCESSING',
    total_records    INT             NOT NULL DEFAULT 0,
    success_records  INT             NOT NULL DEFAULT 0,
    failed_records   INT             NOT NULL DEFAULT 0,
    duplicate_records INT            NOT NULL DEFAULT 0,
    error_file_path  VARCHAR(500),
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    CONSTRAINT pk_file_uploads PRIMARY KEY (id),
    CONSTRAINT fk_file_uploads_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS file_upload_errors (
    id            BIGINT  NOT NULL AUTO_INCREMENT,
    upload_id     BIGINT  NOT NULL,
    `row_number`  INT     NOT NULL,
    row_data      TEXT,
    error_message TEXT    NOT NULL,
    CONSTRAINT pk_file_upload_errors PRIMARY KEY (id),
    CONSTRAINT fk_upload_errors_upload FOREIGN KEY (upload_id) REFERENCES file_uploads (id)
);

-- ── V3: Asset schema ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS stb_assets (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT          NOT NULL,
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
    CONSTRAINT uq_asset_serial_tenant UNIQUE (tenant_id, serial_number),
    CONSTRAINT fk_assets_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants  (id),
    CONSTRAINT fk_assets_retailer FOREIGN KEY (retailer_id) REFERENCES retailers (id)
);

CREATE TABLE IF NOT EXISTS stb_asset_history (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    asset_id      BIGINT       NOT NULL,
    tenant_id     BIGINT       NOT NULL,
    from_status   VARCHAR(20),
    to_status     VARCHAR(20)  NOT NULL,
    retailer_id   BIGINT,
    changed_by    VARCHAR(100),
    remarks       VARCHAR(500),
    changed_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_stb_asset_history PRIMARY KEY (id),
    CONSTRAINT fk_history_asset  FOREIGN KEY (asset_id)  REFERENCES stb_assets (id),
    CONSTRAINT fk_history_tenant FOREIGN KEY (tenant_id) REFERENCES tenants    (id)
);

CREATE TABLE IF NOT EXISTS stb_sales (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT          NOT NULL,
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
    CONSTRAINT fk_sales_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants   (id),
    CONSTRAINT fk_sales_retailer FOREIGN KEY (retailer_id) REFERENCES retailers (id)
);

CREATE TABLE IF NOT EXISTS stb_sale_items (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    sale_id     BIGINT        NOT NULL,
    asset_id    BIGINT        NOT NULL,
    unit_price  DECIMAL(15,2) NOT NULL,
    CONSTRAINT pk_stb_sale_items PRIMARY KEY (id),
    CONSTRAINT uq_sale_item_asset UNIQUE (sale_id, asset_id),
    CONSTRAINT fk_sale_items_sale  FOREIGN KEY (sale_id)  REFERENCES stb_sales  (id),
    CONSTRAINT fk_sale_items_asset FOREIGN KEY (asset_id) REFERENCES stb_assets (id)
);

-- ── V4: Finance schema ────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS financial_transactions (
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

CREATE TABLE IF NOT EXISTS outbox_events (
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

-- ── Indexes (idempotent via stored procedure) ─────────────────────────────────

DROP PROCEDURE IF EXISTS create_index_if_missing;

DELIMITER $$
CREATE PROCEDURE create_index_if_missing(
    IN tbl VARCHAR(100),
    IN idx VARCHAR(100),
    IN ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name   = tbl
          AND index_name   = idx
    ) THEN
        SET @sql = ddl;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL create_index_if_missing('retailers',              'idx_retailers_tenant_status',  'CREATE INDEX idx_retailers_tenant_status ON retailers (tenant_id, status)');
CALL create_index_if_missing('retailers',              'idx_retailers_tenant_mobile',  'CREATE INDEX idx_retailers_tenant_mobile ON retailers (tenant_id, mobile)');
CALL create_index_if_missing('retailers',              'idx_retailers_tenant_name',    'CREATE INDEX idx_retailers_tenant_name ON retailers (tenant_id, retailer_name)');
CALL create_index_if_missing('file_uploads',           'idx_file_uploads_tenant_type', 'CREATE INDEX idx_file_uploads_tenant_type ON file_uploads (tenant_id, upload_type)');
CALL create_index_if_missing('file_upload_errors',     'idx_upload_errors_upload',     'CREATE INDEX idx_upload_errors_upload ON file_upload_errors (upload_id)');
CALL create_index_if_missing('stb_assets',             'idx_assets_tenant_status',     'CREATE INDEX idx_assets_tenant_status ON stb_assets (tenant_id, status)');
CALL create_index_if_missing('stb_assets',             'idx_assets_tenant_retailer',   'CREATE INDEX idx_assets_tenant_retailer ON stb_assets (tenant_id, retailer_id)');
CALL create_index_if_missing('stb_assets',             'idx_assets_tenant_serial',     'CREATE INDEX idx_assets_tenant_serial ON stb_assets (tenant_id, serial_number)');
CALL create_index_if_missing('stb_asset_history',      'idx_asset_history_asset',      'CREATE INDEX idx_asset_history_asset ON stb_asset_history (asset_id)');
CALL create_index_if_missing('stb_asset_history',      'idx_asset_history_tenant',     'CREATE INDEX idx_asset_history_tenant ON stb_asset_history (tenant_id, changed_at)');
CALL create_index_if_missing('stb_sales',              'idx_sales_tenant_retailer',    'CREATE INDEX idx_sales_tenant_retailer ON stb_sales (tenant_id, retailer_id)');
CALL create_index_if_missing('stb_sales',              'idx_sales_tenant_date',        'CREATE INDEX idx_sales_tenant_date ON stb_sales (tenant_id, transaction_date)');
CALL create_index_if_missing('stb_sale_items',         'idx_sale_items_sale',          'CREATE INDEX idx_sale_items_sale ON stb_sale_items (sale_id)');
CALL create_index_if_missing('stb_sale_items',         'idx_sale_items_asset',         'CREATE INDEX idx_sale_items_asset ON stb_sale_items (asset_id)');
CALL create_index_if_missing('financial_transactions', 'idx_finance_tenant_retailer',  'CREATE INDEX idx_finance_tenant_retailer ON financial_transactions (tenant_id, retailer_id)');
CALL create_index_if_missing('financial_transactions', 'idx_finance_tenant_type',      'CREATE INDEX idx_finance_tenant_type ON financial_transactions (tenant_id, transaction_type)');
CALL create_index_if_missing('financial_transactions', 'idx_finance_tenant_status',    'CREATE INDEX idx_finance_tenant_status ON financial_transactions (tenant_id, transaction_status)');
CALL create_index_if_missing('financial_transactions', 'idx_finance_tenant_date',      'CREATE INDEX idx_finance_tenant_date ON financial_transactions (tenant_id, transaction_date)');
CALL create_index_if_missing('financial_transactions', 'idx_finance_sale',             'CREATE INDEX idx_finance_sale ON financial_transactions (sale_id)');
CALL create_index_if_missing('outbox_events', 'idx_outbox_status', 'CREATE INDEX idx_outbox_status ON outbox_events (status, created_at)');

-- ── Flyway schema history ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT            NOT NULL,
    version        VARCHAR(50),
    description    VARCHAR(200)   NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    script         VARCHAR(1000)  NOT NULL,
    checksum       INT,
    installed_by   VARCHAR(100)   NOT NULL,
    installed_on   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT            NOT NULL,
    success        TINYINT(1)     NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);

CALL create_index_if_missing('flyway_schema_history', 'flyway_schema_history_s_idx', 'CREATE INDEX flyway_schema_history_s_idx ON flyway_schema_history (success)');

DROP PROCEDURE IF EXISTS create_index_if_missing;

INSERT IGNORE INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES
    (1, '1', 'platform schema', 'SQL', 'V1__platform_schema.sql', -1, 'setup', 100, 1),
    (2, '2', 'retailer schema', 'SQL', 'V2__retailer_schema.sql', -1, 'setup', 100, 1),
    (3, '3', 'asset schema',    'SQL', 'V3__asset_schema.sql',    -1, 'setup', 100, 1),
    (4, '4', 'finance schema',  'SQL', 'V4__finance_schema.sql',  -1, 'setup', 100, 1);

-- ── Seed: tenant + admin user ─────────────────────────────────────────────────
-- Username: admin   Password: admin123  (BCrypt rounds=10)

INSERT IGNORE INTO tenants (tenant_code, name, status, created_by)
VALUES ('DIST001', 'D2H Distributor', 'ACTIVE', 'setup');

INSERT IGNORE INTO users (username, email, password_hash, full_name, status, created_by)
VALUES (
    'admin',
    'admin@dist001.com',
    '$2a$10$o/PE6rjukQ.9A2vI8xt9e.k.srdbMWqoHt..ZY4omY19Ok3e7KR3e',
    'Admin User',
    'ACTIVE',
    'setup'
);

INSERT IGNORE INTO user_roles (user_id, role_name)
SELECT id, 'TENANT_ADMIN' FROM users WHERE username = 'admin';

INSERT IGNORE INTO user_tenants (user_id, tenant_id)
SELECT u.id, t.id
FROM users u, tenants t
WHERE u.username = 'admin' AND t.tenant_code = 'DIST001';
