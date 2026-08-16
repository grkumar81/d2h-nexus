-- V10__subscription.sql
-- Adds subscription tracking fields to tenants.

ALTER TABLE tenants
    ADD COLUMN subscription_expiry      DATE         NULL AFTER phone,
    ADD COLUMN grace_period_days        INT          NOT NULL DEFAULT 30 AFTER subscription_expiry,
    ADD COLUMN last_expiry_notified_at  DATETIME(6)  NULL AFTER grace_period_days;
