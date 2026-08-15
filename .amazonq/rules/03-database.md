# Database Rules

Use MySQL initially and migrations through Flyway or Liquibase.

Tenant isolation is mandatory.

Before UPDATE/DELETE, verify the corresponding SELECT. Never use unrestricted UPDATE/DELETE.

Use unique constraints for business identifiers. Use appropriate foreign keys and indexes.

Optimize dashboard/report queries with database-side aggregation.

Avoid SELECT * when unnecessary. Avoid N+1.

Use EXPLAIN for slow queries.

Avoid unnecessary MySQL-specific SQL so PostgreSQL migration remains feasible.

Every schema change requires a migration.
