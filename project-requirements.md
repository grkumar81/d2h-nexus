# D2H Distributor Management Platform — Full Requirements

## 1. Business Goal
Build a secure multi-tenant web application for D2H distributors to manage retailers, D2H/STB assets, box sales, financial transactions, recharge transactions, bulk uploads, dashboards, reports, audit history, email notifications and WhatsApp notifications.

The system must answer:
- Which retailers belong to this distributor?
- How many boxes are available?
- How many boxes are allocated/sold/activated?
- Which retailer currently holds an asset?
- How much has been received?
- How much is due/outstanding?
- How much recharge has been completed?
- What happened for a retailer, month, year or date range?
- Who changed financial or asset data?

## 2. Multi-Tenancy
A distributor is a tenant. There is no normal distributor-management module inside a tenant.

Platform administration can:
- register a tenant
- activate/suspend a tenant
- create the initial tenant administrator
- configure tenant settings

Tenant onboarding:
1. Register distributor.
2. Create unique tenant ID.
3. Create tenant database/schema boundary.
4. Apply migrations.
5. Create tenant admin.
6. Configure settings.
7. Activate tenant.
8. Send onboarding information.

Tenant isolation is mandatory. Never trust a tenant ID supplied only by the browser. Resolve tenant from authenticated identity/claims/context. Clear tenant context after each request.

## 3. Database
Use MySQL 8.x initially. Use Spring Data JPA/Hibernate and Flyway or Liquibase.

Design for future PostgreSQL migration:
- avoid unnecessary MySQL-specific behavior
- isolate database-specific SQL
- keep business logic database independent
- use migrations
- use standard JPA where practical

Conceptually:
- platform area: tenants, users, roles, permissions, user-tenants, tenant configuration
- tenant area: retailers, assets, asset history, box sales, finance, adjustments, recharge, uploads, notifications, audit

## 4. Technology
Backend:
- Java 25
- latest stable Spring Boot 4.x available when implementation starts
- Spring Security
- Spring Data JPA
- Hibernate
- Maven
- Jakarta Validation
- REST
- OpenAPI/Swagger
- Actuator/health
- structured logging

Frontend:
- React
- TypeScript
- React Router
- CSS
- reusable components
- AG Grid where useful
- charting library

Deployment:
- Docker/Kubernetes compatible

## 5. Architecture
Start as a modular monolith. Do not introduce microservices without a demonstrated need.

Modules:
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

## 6. Authentication and Authorization
Login:
- username/email
- password
- logout
- forgot/reset password
- failed-login protection
- account status

Use Spring Security. Never store plain-text passwords.

Initial roles:
- TENANT_ADMIN
- FINANCE_USER
- OPERATIONS_USER
- READ_ONLY

Backend authorization is mandatory. The frontend is not a security boundary.

## 7. Retailers
Fields:
- id
- retailer code
- retailer name
- mobile
- alternate mobile
- email
- address
- city
- state
- PIN
- GST/PAN where applicable
- bank information where appropriate
- status
- joining date
- created/modified metadata

Statuses:
- ACTIVE
- INACTIVE
- BLOCKED
- SUSPENDED

Features:
- create/view/update
- activate/deactivate
- search
- filter
- sort
- pagination
- export
- CSV/Excel upload

Search by retailer code, name, mobile, city, state and status.

## 8. Retailer Upload
Support CSV and Excel.

Flow:
Upload -> file validation -> header validation -> data validation -> duplicate validation -> business validation -> staging -> batch processing -> result.

Result:
- total
- inserted
- updated if supported
- failed
- duplicates
- validation errors

Provide downloadable failed records.

Do not load arbitrarily large files entirely into memory.

## 9. STB / D2H Assets
Fields:
- asset ID
- STB serial number
- box number
- model
- manufacturer
- batch
- purchase date
- purchase cost
- status
- current retailer
- tagging date
- sale date
- activation date
- return date
- created/modified metadata

Statuses:
- AVAILABLE
- ALLOCATED
- SOLD
- ACTIVATED
- RETURNED
- DAMAGED
- LOST
- BLOCKED
- SCRAPPED

Rules:
- serial and asset IDs are unique
- unavailable assets cannot be sold
- blocked/damaged/lost assets cannot be assigned
- same asset cannot be active for two retailers
- every state/ownership change is auditable

## 10. Box Sales
Track:
- transaction ID
- transaction date
- retailer
- asset ID
- serial
- quantity
- unit price
- total amount
- payment status
- reference
- remarks
- created metadata

Individual assets must be identifiable.

## 11. Finance
Transactions may be manual or uploaded.

Initial configurable types:
- BOX_SALE
- PAYMENT_RECEIVED
- RECHARGE
- REFUND
- CREDIT
- DEBIT
- ADJUSTMENT
- OTHER

Manual entry:
- transaction ID
- date
- retailer
- type
- amount
- payment method
- reference
- description
- remarks
- attachment reference
- audit metadata

Validate retailer, tenant access, amount, type, date and duplicate references.

## 12. Finance Upload
Support CSV and Excel.

Validate:
- columns
- retailer code
- date
- type
- amount
- reference
- payment mode
- duplicates
- invalid retailer
- invalid amount
- invalid type

Return total/success/failed/duplicates/processed amount/errors.

Large uploads should use controlled batch/asynchronous processing.

## 13. Financial Rules
Define:
- Total Due = amount payable/receivable according to approved business rules.
- Total Received = valid posted payment transactions.
- Outstanding = Total Due - Total Received.
- Recharge is separate unless the business explicitly includes it in balance calculations.

Do not physically delete finalized financial transactions. Use adjustments/reversals and audit.

## 14. Recharge
Fields:
- recharge ID
- date
- retailer
- amount
- reference
- payment mode
- status
- remarks
- audit metadata

Statuses:
- INITIATED
- SUCCESS
- FAILED
- REVERSED
- CANCELLED

## 15. Notifications
Channels:
- Email
- WhatsApp

Events:
- finance transaction posted
- finance transaction updated
- finance upload completed
- finance upload completed with errors
- transaction reversed/adjusted
- optionally recharge posted

Notification configuration must be tenant-aware.

Do not send external notifications directly from the core finance method.

Preferred:
Finance transaction -> database commit -> outbox/event -> notification processor -> Email/WhatsApp.

Notification failure must never roll back a committed financial transaction.

Track notification ID, tenant, event, transaction, channel, recipient, template, status, attempts, timestamps and failure reason.

Statuses:
- PENDING
- PROCESSING
- SENT
- FAILED
- RETRYING
- CANCELLED

Use bounded retries with backoff.

## 16. Email
Use an email provider abstraction. Initial provider may be SMTP, AWS SES, or another approved provider.

Do not hardcode provider-specific logic into finance services.

Notification should include retailer, transaction, amount, reference, total due, total received and outstanding.

## 17. WhatsApp
Use a WhatsApp provider abstraction and an approved WhatsApp Business/API provider.

Keep provider-specific implementation isolated.

WhatsApp message should be concise and contain the updated financial position.

## 18. Upload Notification
After finance upload, notify with:
- file
- total records
- successful
- failed
- total amount processed
- error report availability

Do not claim success until processing result is known.

## 19. Dashboard
Default to current financial year.

Financial KPIs:
- box sales amount
- amount received
- amount due
- outstanding
- recharge
- transaction count

Asset KPIs:
- total
- available
- allocated
- sold
- activated
- returned
- damaged
- lost

Retailer KPIs:
- total
- active
- inactive
- with outstanding balance

Charts:
- monthly box sales
- monthly received
- monthly recharge
- monthly outstanding
- asset distribution
- top received retailers
- top recharge retailers
- highest outstanding retailers

Dashboard must use aggregate backend APIs, not raw transaction downloads.

## 20. Reports
Required:
1. Retailer-wise
2. All-retailer
3. Year-wise
4. Month-wise
5. Date-range

Retailer report:
- box sales
- due
- received
- outstanding
- recharge
- transaction history
- asset history

All-retailer report:
- retailer
- sales
- received
- outstanding
- recharge

Year/month/date reports:
- sales
- received
- outstanding
- recharge
- transaction count

Filters:
- retailer
- financial year
- month
- from/to date
- transaction type
- payment mode
- status

Exports:
- screen
- CSV
- Excel
- PDF if required

Large report generation should be asynchronous.

## 21. UI
Navigation:
- Dashboard
- Retailers
- Assets
- Finance
- Recharge
- Reports
- Administration

Retailers:
- list
- add/edit
- upload

Assets:
- inventory
- tagging
- box sales
- history

Finance:
- transactions
- manual entry
- upload
- outstanding
- adjustments

Recharge:
- transactions
- manual entry
- upload
- history

Reports:
- retailer
- financial
- asset
- recharge

Administration:
- users
- roles
- tenant configuration

Use reusable components, consistent validation, loading/error/empty states and responsive accessible UI.

Use AG Grid for large tables such as retailer, finance, recharge, assets and reports. Use server-side pagination/filtering/sorting for large data.

## 22. REST APIs
Use /api/v1.

Suggested areas:
- /api/v1/auth
- /api/v1/tenants
- /api/v1/users
- /api/v1/retailers
- /api/v1/assets
- /api/v1/box-sales
- /api/v1/finance
- /api/v1/recharges
- /api/v1/uploads
- /api/v1/dashboard
- /api/v1/reports
- /api/v1/notifications
- /api/v1/audit

Use consistent error responses with code, message, timestamp and traceId.

Never expose stack traces or SQL errors.

## 23. Database Tables
Platform:
- tenants
- users
- roles
- permissions
- user_roles
- user_tenants
- tenant_configurations

Tenant:
- retailers
- stb_assets
- stb_asset_history
- stb_sales
- stb_sale_items
- financial_transactions
- financial_transaction_adjustments
- recharge_transactions
- file_uploads
- file_upload_errors
- outbox_events
- notification_delivery
- audit_logs

## 24. Performance
- pagination
- server-side filtering
- projections
- no N+1
- batch processing
- optimized dashboard aggregates
- slow-query monitoring
- controlled connection pool
- no unnecessary full-table/full-file loads

## 25. Security
- HTTPS in deployed environments
- password hashing
- backend authorization
- tenant isolation
- parameterized queries
- input validation
- file size/type validation
- no secrets in code/logs
- no stack traces to clients
- audit sensitive operations

## 26. Testing
Backend unit:
- finance calculations
- validation
- tenant resolution/isolation
- asset lifecycle
- notification events

Integration:
- repositories
- migrations
- tenant schema selection
- finance
- uploads
- outbox

API:
- auth
- authorization
- CRUD
- validation
- errors
- tenant access

Frontend:
- login
- forms
- dashboard
- upload
- filters
- key workflows

E2E:
1. login
2. create retailer
3. upload retailer
4. add asset
5. tag asset
6. sell box
7. record payment
8. verify dashboard
9. verify report
10. verify notification event

## 27. Audit
Audit:
- retailer changes
- asset changes
- box sales
- financial transactions
- adjustments
- recharge
- uploads
- notification configuration
- user/role changes
- tenant configuration

Do not store secrets in audit records.

## 28. Idempotency
Prevent duplicate processing using:
- unique references
- upload IDs
- idempotency keys where appropriate
- database constraints

Repeated uploads/requests must not silently duplicate finance records.

## 29. Implementation Phases
Phase 1: foundation, tenant, auth, MySQL, migrations, React foundation.
Phase 2: retailer and upload.
Phase 3: assets and box sales.
Phase 4: finance and audit.
Phase 5: recharge.
Phase 6: email/WhatsApp/outbox/retry.
Phase 7: dashboard.
Phase 8: reports/exports.
Phase 9: security/performance/tenant-isolation/production hardening.

## 30. Definition of Done
A feature is complete only when:
- requirement implemented
- authorization implemented
- validation implemented
- migration added
- tests added/updated
- relevant tests pass
- API docs updated
- audit added where required
- error handling implemented
- UI validation implemented
- no unrelated changes
- diff reviewed

The AI must never claim a command/test/build/migration/deployment was executed unless it actually executed it.

## 31. Business Questions to Confirm
Before final production behavior is locked:
- exact definition of D2X/STB
- exact financial-year rules
- exact due calculation
- whether recharge affects balance
- STB transfer rules
- financial approval rules
- adjustment/reversal authorization
- payment methods
- WhatsApp provider
- email provider
- notification recipients
- upload size
- expected retailer/transaction scale
- data retention
- PDF requirements
- multi-tenant user membership
- whether historical years are editable

Until confirmed, use documented defaults as configurable assumptions and clearly identify them.
