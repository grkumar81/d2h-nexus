# D2H Project Structure

## Repository Layout
```
D2H/
├── .amazonq/rules/          # Amazon Q project rules (auto-loaded) + memory-bank/
├── agent-prompts/           # Role-specific agent prompts (Backend, DB, UI, Testing)
├── frontend/                # React + TypeScript SPA (Vite)
│   └── src/
│       ├── api/             # Axios API clients per domain (auth, retailers, assets, finance, boxsales)
│       ├── components/      # Shared components (Layout, ProtectedRoute, UploadResultPanel)
│       ├── context/         # AuthContext (JWT state, user/tenant info)
│       ├── pages/           # Feature pages (Login, Retailers, Assets, BoxSales, Finance, Outstanding)
│       └── types/           # Shared TypeScript types (index.ts)
├── src/main/java/org/nexus/d2h/   # Backend — modular monolith
│   ├── auth/                # JWT auth, SecurityConfig, JwtAuthFilter, D2HPrincipal
│   ├── tenant/              # TenantContext (ThreadLocal), TenantContextFilter, TenantRoutingDataSource,
│   │                        #   TenantSchemaService, PlatformTenantController/Service, DataMigrationService
│   ├── user/                # User entity, Role enum, UserRepository
│   ├── retailer/            # Retailer CRUD, RetailerSpecification, RetailerService
│   ├── asset/               # StbAsset, AssetService, AssetSpecification, history
│   ├── boxsale/             # StbSale, StbSaleItem, BoxSaleService
│   ├── finance/             # FinancialTransaction, FinanceService, Specification
│   ├── recharge/            # RechargeTransaction, RechargeService, Specification
│   ├── upload/              # RetailerUploadService, FinanceUploadService, RechargeUploadService (CSV/Excel)
│   ├── notification/        # NotificationConfig, NotificationDelivery, OutboxEvent, NotificationService,
│   │                        #   NotificationEventPublisher, NotificationProcessor
│   ├── dashboard/           # DashboardService
│   ├── report/              # ReportService
│   ├── audit/               # AuditLog, AuditService
│   ├── usermgmt/            # UserManagementService
│   ├── common/              # ApiResponse, ErrorResponse, BaseEntity, GlobalExceptionHandler, TraceIdFilter
│   └── D2HApplication.java
├── src/main/resources/
│   ├── db/migration/        # Flyway versioned migrations (V1–V8, T1–T6 tenant scripts)
│   └── application.properties
├── src/test/java/org/nexus/d2h/   # Unit + slice tests mirroring main structure
├── compose.yaml             # Docker Compose (MySQL)
├── pom.xml
└── project-requirements.md
```

## Backend Module Pattern
Each module is a flat package under `org.nexus.d2h.<module>` containing:
- `<Entity>.java` — JPA entity extending BaseEntity
- `<Entity>Repository.java` — Spring Data JPA repository
- `<Entity>Service.java` — business logic, tenant-scoped operations
- `<Entity>Controller.java` — thin REST controller, delegates to service
- `<Entity>Dto.java` / request records — API boundary objects
- `<Entity>Specification.java` — JPA Criteria for dynamic filtering (where needed)

## Database Schema (Flyway migrations)
| Migration | Tables / Purpose |
|---|---|
| V1 | tenants, roles, users, user_roles, user_tenants, tenant_configurations |
| V2 | retailers (shared schema — migrated to tenant schemas) |
| V3 | stb_assets, stb_asset_history (shared schema — migrated to tenant schemas) |
| V4 | financial_transactions, outbox_events (shared schema — migrated to tenant schemas) |
| V8 | Adds `schema_name` to tenants; seeds PLATFORM_ADMIN role |
| T1–T6 | Per-tenant schema scripts: retailers, stb_assets, stb_asset_history, financial_transactions, recharge_transactions, audit_logs, notification tables (no tenant_id columns) |

Business tables in per-tenant schemas have NO `tenant_id` column — the schema itself is the tenant boundary.
Platform schema (`d2h_platform`) holds: tenants, users, roles only.

## Tenant Isolation Architecture
```
Request → JwtAuthFilter (validates JWT, sets SecurityContext)
        → TenantContextFilter (extracts tenantCode from D2HPrincipal, sets TenantContext ThreadLocal)
        → TenantRoutingDataSource (issues USE d2h_tenant_{tenantCode} on each connection)
        → Controller → Service (no tenantId params — schema routing enforces isolation)
        → TenantContextFilter finally block (clears TenantContext)
```
Tenant identity is never trusted from the browser — always resolved from the authenticated JWT principal.
PLATFORM_ADMIN users have null tenantCode; TenantContextFilter skips schema switching for them.
Platform endpoints at `/api/v1/platform/**` require `PLATFORM_ADMIN` role (`@PreAuthorize`).

### DataMigrationService
Idempotent one-time migration: copies rows from `d2h_platform` shared tables (filtered by `tenant_id`) into per-tenant schemas.
Exposed via `POST /api/v1/platform/tenants/migrate` and `POST /api/v1/platform/tenants/{id}/migrate`.
Skips tables that already have data in the target schema.

## Frontend Architecture
- Vite + React 19 + TypeScript 5
- React Router v6 for navigation; ProtectedRoute guards authenticated pages
- AuthContext provides JWT token, username, tenantCode, roles to the component tree
- Per-domain API modules (e.g., `api/finance.ts`) use a shared Axios client with JWT interceptor
- AG Grid for large data tables with server-side pagination/filtering/sorting
- CSS Modules for component-scoped styles

## Key Cross-Cutting Components
| Component | Location | Purpose |
|---|---|---|
| BaseEntity | common/ | createdAt, updatedAt, createdBy, updatedBy via JPA auditing |
| ApiResponse\<T\> | common/ | Uniform success envelope: `{success, data, message}` |
| ErrorResponse | common/ | Uniform error envelope: `{code, message, timestamp, traceId, fieldErrors}` |
| GlobalExceptionHandler | common/ | Maps all exceptions to ErrorResponse; never exposes stack traces |
| TraceIdFilter | common/ | Injects traceId into MDC for structured logging |
| TenantContext | tenant/ | ThreadLocal tenant isolation; cleared after every request |
| TenantRoutingDataSource | tenant/ | Issues `USE d2h_tenant_{code}` per connection; falls back to platform DS when tenantCode is null |
| DataSourceConfig | tenant/ | `platformDataSource` (HikariCP) + `tenantDataSource`; `app.tenant.schema-routing-enabled` controls routing |
| PlatformJpaConfig | tenant/ | JPA config scanning tenant/user packages; `@Primary` |
| TenantJpaConfig | tenant/ | JPA config scanning all business packages; uses tenantDataSource |
| TenantSchemaService | tenant/ | Runs T1–T6 scripts on new tenant schema creation |
| PlatformTenantController | tenant/ | CRUD + approve/suspend/deactivate + migrate endpoints for PLATFORM_ADMIN |
| DataMigrationService | tenant/ | Idempotent copy from shared schema to per-tenant schemas |
