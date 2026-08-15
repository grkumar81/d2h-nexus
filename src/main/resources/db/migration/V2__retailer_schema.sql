-- V2__retailer_schema.sql
-- Retailer and upload tables. Tenant isolation via tenant_id column.
-- Compatible with MySQL 8.x and PostgreSQL (future migration).

CREATE TABLE retailers (
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

CREATE INDEX idx_retailers_tenant_status ON retailers (tenant_id, status);
CREATE INDEX idx_retailers_tenant_mobile ON retailers (tenant_id, mobile);
CREATE INDEX idx_retailers_tenant_name   ON retailers (tenant_id, retailer_name);

CREATE TABLE file_uploads (
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

CREATE INDEX idx_file_uploads_tenant_type ON file_uploads (tenant_id, upload_type);

CREATE TABLE file_upload_errors (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    upload_id    BIGINT          NOT NULL,
    row_number   INT             NOT NULL,
    row_data     TEXT,
    error_message TEXT           NOT NULL,
    CONSTRAINT pk_file_upload_errors PRIMARY KEY (id),
    CONSTRAINT fk_upload_errors_upload FOREIGN KEY (upload_id) REFERENCES file_uploads (id)
);

CREATE INDEX idx_upload_errors_upload ON file_upload_errors (upload_id);
