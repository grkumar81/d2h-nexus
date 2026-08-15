-- T6__audit_schema.sql
-- Audit log for a tenant schema.
-- No tenant_id column — the schema itself is the tenant boundary.

CREATE TABLE audit_logs (
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

CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_date   ON audit_logs (created_at);
CREATE INDEX idx_audit_user   ON audit_logs (performed_by);
