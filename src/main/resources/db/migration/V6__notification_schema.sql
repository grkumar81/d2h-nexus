-- V6__notification_schema.sql
-- Notification configuration, outbox event extension, and delivery tracking.
-- Tenant isolation via tenant_id on all tables.
-- Compatible with MySQL 8.x and PostgreSQL (future migration).

-- ── Extend outbox_events (stub created in V4) ─────────────────────────────────
ALTER TABLE outbox_events
    ADD COLUMN next_retry_at DATETIME(6),
    ADD COLUMN error_message VARCHAR(1000),
    ADD COLUMN updated_at    DATETIME(6);

CREATE INDEX idx_outbox_pending ON outbox_events (status, next_retry_at);

-- ── Notification configuration (per tenant) ───────────────────────────────────
CREATE TABLE notification_config (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id           BIGINT          NOT NULL,
    event_type          VARCHAR(100)    NOT NULL,
    channel             VARCHAR(20)     NOT NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    recipients          VARCHAR(2000),
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by          VARCHAR(100),
    CONSTRAINT pk_notification_config PRIMARY KEY (id),
    CONSTRAINT uq_notif_config_tenant_event_channel UNIQUE (tenant_id, event_type, channel),
    CONSTRAINT fk_notif_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX idx_notif_config_tenant ON notification_config (tenant_id);

-- ── Notification delivery log ─────────────────────────────────────────────────
CREATE TABLE notification_delivery (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id       BIGINT          NOT NULL,
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
    CONSTRAINT fk_notif_delivery_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_notif_delivery_outbox FOREIGN KEY (outbox_event_id) REFERENCES outbox_events (id)
);

CREATE INDEX idx_notif_delivery_tenant_status ON notification_delivery (tenant_id, status);
CREATE INDEX idx_notif_delivery_outbox        ON notification_delivery (outbox_event_id);
