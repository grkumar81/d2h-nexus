# D2H Distributor Management Platform — Product Overview

## Purpose
Multi-tenant web application for D2H distributors to manage their full business operations: retailers, STB/D2H assets, box sales, financial transactions, recharge, bulk uploads, dashboards, reports, audit history, and notifications (email + WhatsApp).

## Target Users
- **Platform Admin** — cross-tenant platform management (PLATFORM_ADMIN role)
- **Tenant Admin** — full access per distributor tenant
- **Finance User** — financial transactions, uploads, reports
- **Operations User** — assets, box sales, retailer management
- **Read Only** — view-only access across modules

## Key Business Questions the System Answers
- Which retailers belong to this distributor?
- How many boxes are available / allocated / sold / activated?
- Which retailer currently holds an asset?
- How much has been received? How much is outstanding?
- How much recharge has been completed?
- What happened for a retailer, month, year, or date range?
- Who changed financial or asset data?

## Core Modules
| Module | Responsibility |
|---|---|
| auth | JWT login, Spring Security, role enforcement |
| tenant | Multi-tenant isolation, tenant lifecycle |
| user | User management, roles (PLATFORM_ADMIN, TENANT_ADMIN, FINANCE_USER, OPERATIONS_USER, READ_ONLY) |
| retailer | Retailer CRUD, search, status management, CSV/Excel upload |
| asset | STB asset inventory, lifecycle (AVAILABLE→ALLOCATED→SOLD→ACTIVATED→RETURNED), history |
| boxsale | Box sale transactions linking assets to retailers |
| finance | Financial transactions (BOX_SALE, PAYMENT_RECEIVED, RECHARGE, REFUND, CREDIT, DEBIT, ADJUSTMENT, OTHER), upload |
| recharge | Recharge transaction tracking |
| upload | CSV/Excel bulk upload for retailers and finance |
| notification | Email + WhatsApp via outbox pattern; never blocks finance commits |
| dashboard | Aggregate KPIs — financial, asset, retailer; charts |
| report | Retailer-wise, all-retailer, year/month/date-range reports; CSV/Excel/PDF export |
| audit | Immutable audit trail for all sensitive operations |
| common | ApiResponse, ErrorResponse, BaseEntity, GlobalExceptionHandler, TraceIdFilter |

## Financial Rules
- **Outstanding = Total Due − Total Received**
- Recharge is tracked separately unless explicitly included in balance
- Finalized transactions are never physically deleted — use adjustments/reversals
- Notification failure must never roll back a committed financial transaction

## Implementation Phases (current state: Phases 1–4 + Platform Admin + Schema Routing implemented)
1. Foundation, tenant, auth, MySQL, migrations, React foundation
2. Retailer + upload
3. Assets + box sales
4. Finance + audit
5. Recharge
6. Email/WhatsApp/outbox/retry
7. Dashboard
8. Reports/exports
9. Platform Admin role + per-tenant schema routing (schema-per-tenant isolation, DataMigrationService)
10. Security/performance/production hardening *(pending)*
