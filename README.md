# D2H Distributor Management Platform — Amazon Q Project Pack

This package contains the full project requirements, Amazon Q project rules, role-specific agent prompts, and an implementation plan.

## Technology
- Java 25
- Latest stable Spring Boot 4.x available when development starts
- React + TypeScript + CSS
- MySQL 8.x initially
- Spring Data JPA / Hibernate
- Flyway or Liquibase
- REST + OpenAPI
- AG Grid for large/complex tables where useful
- Email + WhatsApp integrations

## Architecture
The application is multi-tenant. A distributor is a tenant. Tenant business data must be isolated by tenant schema/database namespace. Tenant identity must come from authenticated context.

Start as a modular monolith. Keep persistence sufficiently database-agnostic for a future MySQL-to-PostgreSQL migration.

## Important Amazon Q note
Amazon Q Developer automatically uses Markdown project rules placed under `.amazonq/rules/`. The `agent-prompts/` files are role prompts for using the same coding agent as UI, Backend, Database, or Testing specialists; they should not be treated as a claim that Amazon Q IDE provides four independent native coding agents.

## Recommended workflow
1. Add this pack to the repository.
2. Read `project-requirements.md`.
3. Use `amazon-q-start-prompt.md` for repository discovery.
4. Use the specialist prompts for each implementation responsibility.
5. Implement one phase at a time.
6. Run tests and review diffs after each phase.
