# D2H Nexus — Project Phase Analysis & Architecture Decision

## 1. Purpose

This document defines how Amazon Q must analyze, plan, and implement each development phase of D2H Nexus.

Every significant phase must be considered across:
- Database
- Backend
- REST APIs
- Frontend
- Security
- Multi-tenancy
- Testing
- Documentation
- Git

Required workflow:

**ANALYZE → PLAN → IMPACT REVIEW → IMPLEMENT → TEST → REVIEW → REPORT**

Do not implement a phase blindly.

## 2. Product

**Company:** Nexora Digital Systems

**Product:** D2H Nexus

D2H Nexus is a multi-tenant D2H distributor management platform for retailers, D2H/STB assets, box sales, finance, recharge, dashboards, reports, email and WhatsApp notifications.

## 3. Repository Architecture

Use a single Git repository.

```text
D2H/
├── .amazonq/
├── agent-prompts/
├── frontend/
├── src/
├── pom.xml
├── project-requirements.md
├── implementation-plan.md
└── README.md
```

The backend and frontend are separate applications in the same repository.

## 4. Backend Architecture

Technology:
- Java 25
- Latest stable Spring Boot 4.x available when implementation starts
- Spring Security
- Spring Data JPA
- Hibernate
- Maven
- MySQL 8.x
- Flyway or Liquibase
- REST
- OpenAPI
- Actuator

Start as a modular monolith.

Suggested modules:
- auth
- tenant
- user
- retailer
- asset
- boxsale
- finance
- recharge
- upload
- notification
- dashboard
- report
- audit
- common

Do not introduce microservices without a demonstrated requirement.

## 5. Frontend Architecture

### Location

The React application must live at:

```text
frontend/
```

Do not embed the primary React application inside:

```text
src/main/resources/static/
```

### Technology

Use:

- React
- TypeScript
- Vite
- CSS
- AG Grid where appropriate
- a suitable charting library where appropriate

Do not use Create React App.

The frontend must be independently buildable and deployable.

Suggested structure:

```text
frontend/
├── src/
│   ├── api/
│   ├── components/
│   ├── layouts/
│   ├── pages/
│   ├── routes/
│   ├── hooks/
│   ├── types/
│   ├── utils/
│   ├── styles/
│   └── main.tsx
├── public/
├── package.json
├── vite.config.ts
├── tsconfig.json
└── index.html
```

If the existing frontend uses a better established structure, reuse it instead of forcing this exact layout.

## 6. Frontend/Backend Communication

React communicates with Spring Boot through REST APIs.

```text
React
  |
  | HTTPS / REST
  v
Spring Boot
  |
  v
Tenant Context
  |
  v
Business Service
  |
  v
MySQL
```

React must never connect directly to MySQL.

Authoritative financial calculations must be performed by the backend.

## 7. Multi-Tenancy

A distributor is a tenant.

Tenant business data must be isolated according to the tenant architecture established in Phase 1.

Tenant identity must come from the authenticated security context.

Never trust a tenant ID supplied only by the browser.

A user from Tenant A must never access Tenant B data.

Every tenant-sensitive feature must include tenant-isolation tests.

## 8. Database

Initial database: MySQL 8.x.

Keep persistence sufficiently database-independent for future PostgreSQL migration.

Rules:
- Use migrations.
- Use proper indexes.
- Use unique constraints.
- Use foreign keys where appropriate.
- Use DECIMAL for money.
- Avoid unnecessary MySQL-specific SQL.
- Avoid SELECT * when unnecessary.
- Avoid N+1 queries.
- Use server-side aggregation for dashboard/report queries.

## 9. Phase Analysis Process

Before every significant phase Amazon Q must:

### Step 1 — Read requirements

Read:

```text
project-requirements.md
implementation-plan.md
.amazonq/rules/*.md
```

Also read the relevant specialist prompts in:

```text
agent-prompts/
```

### Step 2 — Inspect existing code

Inspect:
- packages
- entities
- repositories
- services
- controllers
- DTOs
- migrations
- security
- tenant implementation
- frontend structure
- frontend components
- API client
- routes
- tests
- configuration
- Docker/Compose
- build configuration

Search for reusable implementations before creating new ones.

## 10. Phase Impact Analysis

Before implementation, identify:

| Area | Existing | Action |
|---|---|---|
| Database | Yes/No | New/Modify/Reuse |
| Entity | Yes/No | New/Modify/Reuse |
| Repository | Yes/No | New/Modify/Reuse |
| Service | Yes/No | New/Modify/Reuse |
| REST API | Yes/No | New/Modify/Reuse |
| Security | Yes/No | New/Modify/Reuse |
| Tenant | Yes/No | New/Modify/Reuse |
| Frontend page | Yes/No | New/Modify/Reuse |
| Frontend component | Yes/No | New/Modify/Reuse |
| API integration | Yes/No | New/Modify/Reuse |
| Tests | Yes/No | New/Modify/Reuse |
| Audit | Yes/No | New/Modify/Reuse |

Amazon Q should provide this analysis before implementing a significant phase.

## 11. Dependency Analysis

Identify dependencies on previous phases.

Expected business flow:

```text
Retailer
   |
   v
Asset
   |
   v
Box Sale
   |
   v
Finance
   |
   ├── Notifications
   ├── Dashboard
   └── Reports
```

Reuse existing modules. Never create duplicate Retailer, Asset or Box Sale models.

## 12. API Impact Analysis

Before creating an endpoint:
1. Search for an existing endpoint.
2. Search for existing DTOs.
3. Search for existing service methods.
4. Follow `/api/v1`.
5. Follow existing validation and error handling.
6. Apply authorization.
7. Apply tenant isolation.
8. Use existing pagination/filter/sort conventions.

Do not create duplicate APIs.

## 13. Database Impact Analysis

Before creating a table:
1. Search for an existing table.
2. Check relationships.
3. Check existing migrations.
4. Identify indexes.
5. Identify unique constraints.
6. Identify tenant isolation.
7. Identify audit requirements.

Every schema change requires a migration.

## 14. Frontend Impact Analysis

Before creating a page/component:
1. Search existing pages.
2. Search reusable forms.
3. Search reusable tables.
4. Search AG Grid configuration.
5. Search API clients.
6. Search routes.
7. Search permission handling.
8. Search toast/error components.
9. Search existing styles.

Reuse existing UI patterns.

## 15. Full-Stack Phase Requirement

A phase is complete only when all required layers are implemented.

Typical flow:

```text
Requirement
    |
    v
Database
    |
    v
Backend
    |
    v
REST API
    |
    v
Frontend
    |
    v
Validation
    |
    v
Authorization
    |
    v
Audit
    |
    v
Testing
```

Do not leave required frontend work for a later phase.

## 16. Security Analysis

Every phase must review:
- authentication
- authorization
- tenant isolation
- input validation
- file validation
- sensitive data exposure
- logging
- audit
- secrets

Every new endpoint requires an authorization decision.

## 17. Financial Feature Rules

For finance:
- Use BigDecimal.
- Use DECIMAL in MySQL.
- Never use floating point for authoritative financial calculations.
- Prevent duplicate transactions.
- Never physically delete finalized transactions.
- Use adjustments/reversals.
- Centralize financial calculations.
- Make operations transactional.
- Consider concurrent requests.
- Audit financial changes.

Frontend totals are presentation only.

## 18. Upload Rules

For CSV/Excel:

```text
Upload
  |
  v
File Validation
  |
  v
Header Validation
  |
  v
Row Validation
  |
  v
Duplicate Detection
  |
  v
Business Validation
  |
  v
Batch Processing
  |
  v
Result
```

Do not load arbitrarily large files entirely into memory.

Track:
- upload ID
- filename
- user
- tenant
- status
- total records
- success count
- failure count
- duplicate count
- errors

## 19. Notification Rules

Do not call external Email/WhatsApp providers directly inside core financial transactions.

Preferred:

```text
Finance Transaction
       |
       v
Database Transaction
       |
       v
Outbox Event
       |
       v
Notification Processor
       |
       ├── Email
       └── WhatsApp
```

External notification failure must not roll back a committed financial transaction.

## 20. Dashboard Rules

Dashboard APIs must use backend aggregation.

Do not download millions of transactions into React and calculate totals in the browser.

```text
React Dashboard
       |
       v
Dashboard API
       |
       v
Aggregate SQL
       |
       v
MySQL
```

## 21. Report Rules

Reports must use server-side filtering, aggregation and pagination where appropriate.

Do not load huge reports completely into browser memory.

## 22. Testing Analysis

For every phase identify:
- unit tests
- integration tests
- API tests
- frontend tests
- end-to-end tests
- regression tests

For multi-tenancy always test:

```text
Tenant A → can access Tenant A
Tenant A → cannot access Tenant B
```

## 23. Git Safety

Before implementation:

```bash
git status
git branch
```

Never overwrite existing uncommitted user changes.

Do not use without explicit authorization:
- git reset --hard
- git clean -fd
- git push --force
- history rewriting

At completion:
1. Run tests.
2. Run build.
3. Review diff.
4. Review status.
5. Commit the phase.
6. Push the feature branch when approved.

Do not automatically push to main.

## 24. Phase Completion Criteria

A phase is complete only when:
- database changes implemented
- migrations created
- backend implemented
- APIs implemented
- frontend implemented where required
- validation implemented
- authorization implemented
- tenant isolation verified
- audit implemented where required
- tests implemented
- tests executed
- build executed
- Git diff reviewed
- no unrelated changes
- known issues documented

Amazon Q must not call a phase complete merely because the code compiles.

## 25. Required Phase Completion Report

Amazon Q must report:

### Implementation
- files created
- files modified
- modules changed
- migrations
- APIs
- frontend pages/components

### Security
- permissions
- tenant isolation
- authentication impact

### Testing
- tests added
- tests executed
- results
- failures

### Build
- backend result
- frontend result

### Git
- branch
- changed files
- commit hash if committed
- push status if pushed

### Issues
- known issues
- assumptions
- technical debt
- follow-up work

Never claim a command/test/build/push succeeded unless actually executed.

## 26. Phase Status

| Phase | Feature | Status |
|---|---|---|
| 1 | Foundation, Security, Multi-Tenancy | COMPLETED |
| 2 | Retailer Management | COMPLETED |
| 3 | Asset Management + Box Sales | COMPLETED |
| 4 | Finance Management | COMPLETED |
| 5 | Recharge | COMPLETED |
| 6 | Email + WhatsApp Notifications | COMPLETED |
| 7 | Dashboard | COMPLETED |
| 8 | Reports + Exports | CURRENT |
| 9 | Production Hardening | PLANNED |

## 27. Current Phase — Finance

Phase 4 is full-stack:

```text
MySQL
  ↓
Spring Boot
  ↓
REST API
  ↓
React + TypeScript + Vite
  ↓
Tests
```

Required:
- manual finance entry
- finance transaction list
- finance details
- finance search/filter
- finance upload
- upload validation
- duplicate prevention
- amount due
- amount received
- outstanding
- adjustments
- reversals
- retailer financial summary
- outstanding UI
- audit
- authorization
- tenant isolation

Do not implement Recharge, Notifications, Dashboard or Reports in Phase 4.

## 28. React Architecture Decision

Selected frontend architecture:

**React + TypeScript + Vite**

Location:

```text
frontend/
```

Do not use:
- Create React App
- Spring Boot static resources as the primary React application
- a separate frontend repository

The frontend must remain independently buildable.

## 29. Future Deployment

The architecture should support:

```text
                 Load Balancer
                      |
             ┌────────┴────────┐
             |                 |
             v                 v
      React Frontend     Spring Boot API
       Static Hosting          |
                               v
                             MySQL
```

Frontend and backend should not have unnecessary runtime coupling.

## 30. Final Rule

For every significant phase:

**ANALYZE → PLAN → IMPACT REVIEW → IMPLEMENT → TEST → REVIEW → REPORT**

Prefer understanding and reusing existing code over generating duplicate implementations.
