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
│   ├── tenant/              # TenantContext (ThreadLocal), TenantContextFilter
│   ├── user/                # User entity, Role enum, UserRepository
│   ├── retailer/            # Retailer CRUD, RetailerSpecification, RetailerService
│   ├── asset/               # StbAsset, AssetService, AssetSpecification, history
│   ├── boxsale/             # StbSale, StbSaleItem, BoxSaleService
│   ├── finance/             # FinancialTransaction, FinanceService, Specification
│   ├── upload/              # RetailerUploadService, FinanceUploadService (CSV/Excel)
│   ├── common/              # ApiResponse, ErrorResponse, BaseEntity, GlobalExceptionHandler, TraceIdFilter
│   └── D2HApplication.java
├── src/main/resources/
│   ├── db/migration/        # Flyway versioned migrations (V1–V4)
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
| Migration | Tables |
|---|---|
| V1 | tenants, roles, users, user_roles, user_tenants, tenant_configurations |
| V2 | retailers |
| V3 | stb_assets, stb_asset_history |
| V4 | financial_transactions, outbox_events |

All tenant business tables carry a `tenant_id` FK to `tenants`. Unique constraints enforce business identifiers (e.g., `uq_finance_tenant_reference`).

## Tenant Isolation Architecture
```
Request → JwtAuthFilter (validates JWT, sets SecurityContext)
        → TenantContextFilter (extracts tenantCode from D2HPrincipal, sets TenantContext ThreadLocal)
        → Controller → Service (queries always include tenantId from TenantContext)
        → TenantContextFilter finally block (clears TenantContext)
```
Tenant identity is never trusted from the browser — it is always resolved from the authenticated JWT principal.

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
