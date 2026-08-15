-- V1__platform_schema.sql
-- Platform-level tables shared across all tenants.
-- Tenant business data tables will be added in later migrations.
-- Designed to be compatible with both MySQL 8.x and PostgreSQL (future migration).

CREATE TABLE tenants (
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

-- Reference table for valid role names
CREATE TABLE roles (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    name  VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE users (
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

-- Stores role names directly on the user for efficient auth lookups
CREATE TABLE user_roles (
    user_id    BIGINT      NOT NULL,
    role_name  VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_name),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_tenants (
    user_id    BIGINT  NOT NULL,
    tenant_id  BIGINT  NOT NULL,
    CONSTRAINT pk_user_tenants PRIMARY KEY (user_id, tenant_id),
    CONSTRAINT fk_user_tenants_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_user_tenants_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE tenant_configurations (
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

-- Seed valid role names
INSERT INTO roles (name) VALUES ('TENANT_ADMIN');
INSERT INTO roles (name) VALUES ('FINANCE_USER');
INSERT INTO roles (name) VALUES ('OPERATIONS_USER');
INSERT INTO roles (name) VALUES ('READ_ONLY');
