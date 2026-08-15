# Architecture Rules

Use Java 25, latest stable Spring Boot 4.x available at implementation time, React + TypeScript, MySQL initially, JPA/Hibernate and Flyway/Liquibase.

Start as a modular monolith.

Modules:
auth, tenant, user, retailer, asset, boxsale, finance, recharge, upload, notification, dashboard, report, audit, common.

A distributor is a tenant.

Tenant identity must come from authenticated context. Never trust a browser-supplied tenant ID.

Every tenant business operation must execute against the correct tenant schema/database namespace. Clear tenant context after the request.

Keep persistence sufficiently database-agnostic for future MySQL-to-PostgreSQL migration.

Do not introduce microservices without a demonstrated requirement.
