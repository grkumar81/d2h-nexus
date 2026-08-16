# D2H Development Guidelines

## Backend Patterns

### Constructor Injection (universal)
All Spring beans use constructor injection — never field injection.
```java
public FinanceService(FinancialTransactionRepository txRepository,
                      RetailerRepository retailerRepository) {
    this.txRepository = txRepository;
    ...
}
```
Services no longer inject `TenantRepository` — tenant isolation is handled by schema routing.

### Tenant Context Check (services that require an active tenant)
Services validate that a tenant context is set but do NOT resolve the Tenant entity or pass tenantId to queries.
Schema routing (TenantRoutingDataSource) enforces isolation automatically.
```java
private void ensureTenantContext() {
    String tenantCode = TenantContext.getCurrentTenant();
    if (tenantCode == null || tenantCode.isBlank()) {
        throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
    }
}
```
PLATFORM_ADMIN endpoints skip this check and operate on the platform datasource.

### Repository Lookups (schema-routed — no tenantId param)
All entity lookups use plain `findById()` — the active schema already scopes to the correct tenant:
```java
// Correct — schema routing enforces tenant isolation
txRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FinancialTransaction", id));

retailerRepository.findById(retailerId)
        .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));
```
Never add `tenantId` parameters to repository methods or query methods.

### BusinessException Pattern
Use `BusinessException(code, message)` for domain rule violations (maps to HTTP 422):
```java
throw new BusinessException("ALREADY_REVERSED", "Transaction has already been reversed");
throw new BusinessException("DUPLICATE_REFERENCE", "Reference '" + ref + "' already exists");
throw new BusinessException("EMPTY_FILE", "Uploaded file is empty");
```
Use `ResourceNotFoundException` for missing entities (maps to HTTP 404).

### Transaction Annotations
- `@Transactional` on write methods
- `@Transactional(readOnly = true)` on all read/query methods
- Financial operations are atomic — never split a finance write across multiple transactions

### Logging Pattern (`@Slf4j`)
Log significant state changes with structured key=value pairs. Never log passwords, tokens, or PII.
```java
log.info("Finance tx created: id={} type={} amount={} retailer={} tenant={}",
        saved.getId(), saved.getTransactionType(), saved.getAmount(),
        retailer.getRetailerCode(), tenant.getTenantCode());
```

### Financial Calculations
- Always use `BigDecimal` for money — never `double` or `float`
- Aggregate calculations are done in the database (repository `@Query` methods), not in Java loops
- Outstanding = totalBoxSales − totalReceived (computed server-side, never in the browser)
- Finalized transactions are never deleted; use REVERSAL or ADJUSTMENT transaction types

### Reversal / Adjustment Pattern
```java
// Reversal: negate the original amount, link via reversalOf/reversedBy
reversal.setReversalOf(original);
original.setTransactionStatus(TransactionStatus.REVERSED);
original.setReversedBy(savedReversal);
// Both saves in the same @Transactional method
```

### Reference Deduplication
Unique references are enforced at both DB level (`UNIQUE` constraint) and service level:
```java
if (txRepository.existsByReference(ref)) {
    throw new BusinessException("DUPLICATE_REFERENCE", "...");
}
```
Auto-generate references when none provided: `"MAN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase()`

### Upload Services (CSV + Excel)
- Validate file type by extension before processing
- Build a `Map<String, Integer>` header index (lowercase, trimmed) for column-position-independent parsing
- Validate required headers before processing any rows
- Use `BATCH_SIZE = 100` with `saveAll()` — never save row-by-row
- Track `int[] counters = {total, success, duplicate}` for result reporting
- Use `Set<String>` for in-file duplicate detection before hitting the DB
- Per-row errors are collected in `List<UploadResult.RowError>` — never abort the whole upload on a single row failure
- Log completion with total/success/failed/duplicate/tenant
```java
if (batch.size() >= BATCH_SIZE) {
    retailerRepository.saveAll(batch);
    counters[1] += batch.size();
    batch.clear();
}
```

### AuditService
- Uses `REQUIRES_NEW` propagation — audit writes never roll back with the calling transaction
- Swallows exceptions silently — audit failure must not affect business operations
- `record(String entityType, String entityId, String action, String details, String ipAddress)` — no tenantId param
- Audit logs are stored in the tenant schema (schema routing handles isolation)

### NotificationEventPublisher
- `publish(NotificationEventType eventType, String aggregateId, Map<String, Object> payload)` — no tenantId param
- Notification failure must never roll back a committed financial transaction (outbox pattern)

### Security Configuration
- Stateless JWT — `SessionCreationPolicy.STATELESS`, CSRF disabled
- Filter order: `JwtAuthFilter` → `TenantContextFilter` → `UsernamePasswordAuthenticationFilter`
- Public endpoints: `POST /api/v1/auth/login`, `/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/swagger-ui/**`
- All other requests require authentication
- `@EnableMethodSecurity` enables `@PreAuthorize` for method-level role checks
- CORS origins are externalized to `app.cors.allowed-origins` (never hardcoded)
- Custom `authenticationEntryPoint` returns JSON `{"code":"UNAUTHORIZED","message":"..."}` — never a redirect

### Exception Handler Mapping (`GlobalExceptionHandler`)
| Exception | HTTP Status | Error Code |
|---|---|---|
| MethodArgumentNotValidException | 400 | VALIDATION_ERROR |
| BadCredentialsException | 401 | INVALID_CREDENTIALS |
| LockedException | 401 | ACCOUNT_LOCKED |
| DisabledException | 401 | ACCOUNT_DISABLED |
| AccessDeniedException | 403 | ACCESS_DENIED |
| ResourceNotFoundException | 404 | NOT_FOUND |
| BusinessException | 422 | (from exception) |
| Exception (catch-all) | 500 | INTERNAL_ERROR |

The catch-all logs the full stack trace server-side but returns only a generic message to the client — never expose stack traces.

### API Response Envelopes
Success responses use `ApiResponse<T>`:
```java
// Controller pattern
return ResponseEntity.ok(ApiResponse.success(service.create(request)));
```
Error responses use `ErrorResponse` with `code`, `message`, `timestamp`, `traceId`, and optional `fieldErrors`.

### Annotations Commonly Used
- `@Slf4j` — Lombok logger on every service and upload class
- `@Service`, `@Repository`, `@RestController`, `@Configuration` — standard Spring stereotypes
- `@Transactional` / `@Transactional(readOnly = true)` — on all service methods
- `@RestControllerAdvice` — GlobalExceptionHandler only
- `@EnableWebSecurity`, `@EnableMethodSecurity` — SecurityConfig only
- `@Value("${property:default}")` — externalized config injection

---

## Frontend Patterns

### Typed API Models
All API shapes are defined in `frontend/src/types/index.ts`. Use `interface` for object shapes, `type` for union literals:
```typescript
export type TransactionType = 'BOX_SALE' | 'PAYMENT_RECEIVED' | 'RECHARGE' | ...
export interface FinancialTransaction { id: number; transactionType: TransactionType; ... }
```
Never use `any`. Use `number | null` for optional numeric fields.

### Pagination Type
```typescript
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
```
All paginated API responses use this generic wrapper.

### Auth Context
JWT token, username, tenantCode, and roles are stored in `AuthContext`. Components read auth state via `useContext(AuthContext)` — never from localStorage directly.

### AG Grid Usage
Use AG Grid for large tables (retailers, finance, assets, reports) with server-side pagination/filtering/sorting. Never load all records into the browser.

### CSS Modules
Each page/component has a co-located `.module.css` file. Import as `import styles from './Page.module.css'` and apply as `className={styles.container}`.

---

## Database Patterns

### Tenant Isolation in Schema
Tenant business tables in per-tenant schemas (`d2h_tenant_{code}`) have NO `tenant_id` column.
The schema itself is the tenant boundary — enforced by `TenantRoutingDataSource`.
Unique constraints are simple (no tenant_id prefix):
```sql
CONSTRAINT uq_finance_reference UNIQUE (reference)
```

### Index Strategy
Indexes do not need to lead with `tenant_id` (no longer present in business tables):
```sql
CREATE INDEX idx_finance_retailer ON financial_transactions (retailer_id);
CREATE INDEX idx_finance_date     ON financial_transactions (transaction_date);
```

### Migration Naming
Flyway versioned migrations: `V{n}__{description}.sql`. Every schema change requires a new migration — never modify existing ones.

### PostgreSQL Compatibility
- Avoid `AUTO_INCREMENT` in new SQL (use standard sequences or let JPA handle it)
- Avoid MySQL-specific functions in business queries
- Use `DATETIME(6)` for timestamps (maps to `TIMESTAMP(6)` in PostgreSQL)

---

## Testing Patterns

### Controller Slice Tests (`@WebMvcTest`)
```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;   // Spring Boot 4.x annotation
    // ...
}
```
- Use `@MockitoBean` (Spring Boot 4.x) — not `@MockBean`
- Assert both HTTP status and JSON body fields with `jsonPath`
- Test happy path, validation errors (400), auth errors (401), and not-found (404/422)

### Test Configuration
- H2 in-memory with `MODE=MySQL` for all tests
- Flyway disabled; Hibernate `create-drop` manages schema
- `app.tenant.schema-routing-enabled=false` — H2 does not support `USE` statements
- JWT secret provided in `src/test/resources/application.properties`
- `@MockitoBean` used for service/repository dependencies in controller slice tests

### Naming Convention
Test methods: `methodName_condition_expectedOutcome`
```java
void login_validCredentials_returns200WithToken()
void login_blankUsername_returns400WithFieldError()
```
