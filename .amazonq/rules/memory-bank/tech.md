# D2H Technology Stack

## Backend
| Concern | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Web | spring-boot-starter-webmvc |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8.4 (production), H2 in-memory (tests) |
| Migrations | Flyway (flyway-core + flyway-mysql) |
| Validation | Jakarta Validation (spring-boot-starter-validation) |
| API Docs | springdoc-openapi-starter-webmvc-ui 2.8.8 → `/swagger-ui.html` |
| Monitoring | Spring Actuator → `/actuator/health`, `/actuator/info` |
| HTTP Client | spring-boot-starter-restclient (for future notification providers) |
| Excel/CSV | Apache POI 5.3.0 (poi-ooxml), OpenCSV 5.9 |
| Boilerplate | Lombok |
| Build | Maven (mvnw wrapper) |

## Frontend
| Concern | Technology |
|---|---|
| Language | TypeScript 5.7 |
| Framework | React 19 |
| Build | Vite 6 |
| Routing | React Router DOM 6 |
| HTTP | Axios 1.7 |
| Data Grid | AG Grid Community 33 (ag-grid-react) |
| Styles | CSS Modules |

## Infrastructure
| Concern | Detail |
|---|---|
| Local DB | Docker Compose — `mysql:8.4`, database `d2h_platform`, port 3306 |
| Dev server | Vite on port 5173; proxies `/api` → `http://localhost:8080` |
| Backend port | 8080 (Spring Boot default) |

## Key Configuration
```properties
# Datasource (env-overridable)
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/d2h_platform?...}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}

# JWT (must override in production)
app.jwt.secret=${JWT_SECRET:change-this-secret-...}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

# CORS
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173}

# JPA — schema managed by Flyway, not Hibernate DDL
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

## Development Commands
```bash
# Start MySQL via Docker Compose
docker compose up -d

# Run backend (from repo root)
./mvnw spring-boot:run

# Run all backend tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=FinanceServiceTest

# Frontend dev server (from frontend/)
npm install
npm run dev

# Frontend production build
npm run build
```

## Test Configuration
- Slice tests (`@WebMvcTest`) use H2 in-memory with `MODE=MySQL`
- Flyway disabled in tests; Hibernate `create-drop` manages schema
- JWT secret for tests: base64-encoded 256-bit test key in `src/test/resources/application.properties`
- `@MockitoBean` used for service/repository dependencies in controller slice tests

## API Base Path
All REST endpoints: `/api/v1/`

Documented areas: `/api/v1/auth`, `/api/v1/retailers`, `/api/v1/assets`, `/api/v1/box-sales`, `/api/v1/finance`, `/api/v1/uploads`
