-- T5__notification_schema.sql
-- Notification configuration and delivery tracking for a tenant schema.
-- No tenant_id column — the schema itself is the tenant boundary.

CREATE TABLE notification_config (
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

CREATE TABLE notification_delivery (
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
    CONSTRAINT fk_notif_delivery_outbox FOREIGN KEY (outbox_event_id) REFERENCES outbox_events (id)
);

CREATE INDEX idx_notif_delivery_status ON notification_delivery (status);
CREATE INDEX idx_notif_delivery_outbox ON notification_delivery (outbox_event_id);
