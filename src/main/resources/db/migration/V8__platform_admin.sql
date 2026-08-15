-- V8__platform_admin.sql
-- Adds schema_name to tenants for schema-per-tenant isolation.
-- Seeds PLATFORM_ADMIN role.

ALTER TABLE tenants
    ADD COLUMN schema_name VARCHAR(64) NOT NULL DEFAULT '' AFTER name;

ALTER TABLE tenants
    ADD CONSTRAINT uq_tenants_schema_name UNIQUE (schema_name);

-- Back-fill schema_name for any existing tenants
UPDATE tenants
SET schema_name = CONCAT('d2h_tenant_', tenant_code)
WHERE schema_name = '';

-- Remove the DEFAULT now that existing rows are populated
ALTER TABLE tenants
    ALTER COLUMN schema_name DROP DEFAULT;

-- Seed PLATFORM_ADMIN role
INSERT INTO roles (name) VALUES ('PLATFORM_ADMIN');
