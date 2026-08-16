-- V9__contact_details.sql
-- Adds email and phone contact fields to tenants and users.

ALTER TABLE tenants
    ADD COLUMN email VARCHAR(255) NULL AFTER name,
    ADD COLUMN phone VARCHAR(30)  NULL AFTER email;

ALTER TABLE users
    ADD COLUMN phone VARCHAR(30) NULL AFTER full_name;
