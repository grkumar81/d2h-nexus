# Backend Rules

Use Java 25 and Spring Boot 4.x.

Use constructor injection. Keep controllers thin. Put business logic in services/domain components. Keep database access in repositories.

Use DTOs at API boundaries. Validate inputs. Use centralized exception handling.

Use BigDecimal for money.

Financial operations must be atomic. Prevent duplicates. Do not physically delete finalized financial transactions; use adjustment/reversal.

Do not call email/WhatsApp providers directly from finance services. Use application/domain events and preferably an outbox.

Notification failure must not roll back a committed financial transaction.

Use /api/v1. Enforce authorization server-side. Never expose stack traces or SQL errors.

Use pagination, projections and batching. Avoid N+1 queries.
