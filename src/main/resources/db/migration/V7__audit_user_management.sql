-- V7__audit_user_management.sql
-- Audit log for sensitive operations. Tenant-scoped, immutable (no UPDATE/DELETE).
-- Compatible with MySQL 8.x and PostgreSQL (future migration).

CREATE TABLE audit_logs (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id     BIGINT          NOT NULL,
    entity_type   VARCHAR(100)    NOT NULL,
    entity_id     VARCHAR(100)    NOT NULL,
    action        VARCHAR(50)     NOT NULL,
    performed_by  VARCHAR(100)    NOT NULL,
    details       TEXT,
    ip_address    VARCHAR(45),
    created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX idx_audit_tenant_entity  ON audit_logs (tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_tenant_date    ON audit_logs (tenant_id, created_at);
CREATE INDEX idx_audit_tenant_user    ON audit_logs (tenant_id, performed_by);
