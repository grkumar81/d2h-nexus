Phase 1 — Add PLATFORM_ADMIN role + tenant provisioning fields
Files changed: Role.java, V1__platform_schema.sql (seed), new V8__platform_admin.sql

Add PLATFORM_ADMIN to Role enum

Add schema_name column to tenants table (stores d2h_tenant_{tenantCode})

Add PLATFORM_ADMIN seed to roles table

TenantStatus already has PENDING — add APPROVED if not present (check current enum)

Phase 2 — Routing DataSource infrastructure
New files: TenantRoutingDataSource.java, DataSourceConfig.java

TenantRoutingDataSource extends AbstractRoutingDataSource — determineCurrentLookupKey() returns TenantContext.getCurrentTenant()

DataSourceConfig — defines platformDataSource (HikariCP, d2h_platform), tenantRoutingDataSource (wraps same pool, executes USE d2h_tenant_{key} via afterPropertiesSet)

Single pool strategy: routing datasource uses the same underlying connection pool, switches schema with USE statement

Phase 3 — Dual JPA configuration
New files: PlatformJpaConfig.java, TenantJpaConfig.java

PlatformJpaConfig — EntityManagerFactory + TransactionManager for d2h_platform, scans org.nexus.d2h.tenant, org.nexus.d2h.user, org.nexus.d2h.auth (User entity), org.nexus.d2h.common

TenantJpaConfig — EntityManagerFactory + TransactionManager for routing datasource, scans all business packages: retailer, asset, boxsale, finance, recharge, notification, upload, audit, report, dashboard

@Primary on platform config (used by Spring Security user lookup)

All business services annotated with @Transactional("tenantTransactionManager")

Phase 4 — Tenant schema provisioning service
New files: TenantSchemaService.java, tenant migration scripts T1 through T6

TenantSchemaService — creates d2h_tenant_{tenantCode} schema, runs tenant Flyway migrations programmatically against it

Tenant migrations T1–T6: copies of V2–V7 with tenant_id column and all FKs to tenants removed

Called when tenant status transitions to APPROVED

Also handles data migration: copies existing shared-schema rows for the tenant into its new schema

Phase 5 — Update TenantContextFilter + AuthService
Files changed: TenantContextFilter.java, AuthService.java, D2HPrincipal.java

TenantContextFilter — skip schema switching when tenantCode is null (PLATFORM_ADMIN requests)

AuthService — PLATFORM_ADMIN users have no tenant association; tenantCode stays null in JWT

D2HPrincipal — no change needed (already handles null tenantCode)

Phase 6 — Remove tenant_id from all business entities + repositories
Files changed: All business entities (Retailer, StbAsset, StbAssetHistory, StbSale, FinancialTransaction, RechargeTransaction, OutboxEvent, NotificationConfig, NotificationDelivery, AuditLog) and their repositories

Remove tenantId field from every entity

Remove findByIdAndTenantId → replace with findById

Remove tenant_id param from all @Query methods in repositories

Remove resolveTenant() calls from services (tenant is implicit via schema routing)

Phase 7 — Update all services
Files changed: RetailerService, AssetService, BoxSaleService, FinanceService, RechargeService, NotificationService, AuditService, DashboardService, ReportService, UserManagementService, all upload services

Remove resolveTenant() calls

Remove tenantId parameters from all repo calls

AuditService — tenant_id column gone; log still records performedBy and entity info

All @Transactional → @Transactional("tenantTransactionManager")

Phase 8 — Platform admin endpoints
New files: PlatformTenantController.java, PlatformTenantService.java, TenantDto.java, ApproveTenantRequest.java

POST /api/v1/platform/tenants — register new tenant (PLATFORM_ADMIN only)

POST /api/v1/platform/tenants/{id}/approve — approve tenant → triggers schema provisioning

GET /api/v1/platform/tenants — list all tenants

All secured with @PreAuthorize("hasRole('PLATFORM_ADMIN')")

Phase 9 — Data migration
New file: DataMigrationService.java (run-once, triggered manually or at startup if flag set)

Reads all existing tenants from d2h_platform.tenants

For each tenant: provisions schema if not exists, copies all rows from shared tables filtered by tenant_id into the tenant schema (without tenant_id column)

Idempotent — skips if target schema already has data

Phase 10 — Tests + cleanup
Files changed: All test files, application.properties (test), new V8 migration

Update @WebMvcTest slice tests — remove tenantId from mock setups

Update FinanceServiceTest and other service tests

Remove tenant_id from H2 test schema

Verify 211+ tests still pass