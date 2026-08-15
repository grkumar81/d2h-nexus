# Implementation Plan

## Phase 0 — Discovery
Inspect repository structure, versions, database, authentication, frontend, tests, CI/CD, Docker/Kubernetes and existing reusable components. Do not modify code.

## Phase 1 — Foundation
Implement Java 25/Spring Boot, MySQL, migrations, security foundation, tenant model/context/resolution, React structure, routing, common UI, errors, logging and health checks.

## Phase 2 — Retailers
CRUD, search/filter, pagination, CSV/Excel upload, validation and export.

## Phase 3 — Assets
Inventory, tagging, lifecycle, box sales and history.

## Phase 4 — Finance
Manual entry, upload, validation, lifecycle, received/due/outstanding, adjustments and audit.

## Phase 5 — Recharge
Manual entry, upload, history and status.

## Phase 6 — Notifications
Outbox, events, email, WhatsApp, templates, retries and history.

## Phase 7 — Dashboard
Current-year KPIs, monthly trends, asset distribution and retailer rankings.

## Phase 8 — Reports
Retailer, all-retailer, year, month, date-range, CSV, Excel and optional PDF.

## Phase 9 — Hardening
Security, performance, large uploads, tenant isolation, notification failure, backup/restore and migration readiness.

Do not generate the whole application in one uncontrolled change. Implement and validate one phase at a time.
