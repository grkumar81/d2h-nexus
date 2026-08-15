package org.nexus.d2h.tenant;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Creates and initialises a tenant schema when a tenant is approved.
 * Executes T1–T6 migration scripts from {@code db/tenant-migration/} in order.
 */
@Slf4j
@Service
public class TenantSchemaService {

    private static final List<String> MIGRATION_SCRIPTS = List.of(
            "db/tenant-migration/T1__retailer_schema.sql",
            "db/tenant-migration/T2__asset_schema.sql",
            "db/tenant-migration/T3__finance_schema.sql",
            "db/tenant-migration/T4__recharge_schema.sql",
            "db/tenant-migration/T5__notification_schema.sql",
            "db/tenant-migration/T6__audit_schema.sql"
    );

    private final DataSource platformDataSource;
    private final TenantRepository tenantRepository;

    public TenantSchemaService(@Qualifier("platformDataSource") DataSource platformDataSource,
                               TenantRepository tenantRepository) {
        this.platformDataSource = platformDataSource;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Creates the tenant schema and runs all T-scripts.
     * Called when a tenant transitions to APPROVED status.
     * Idempotent — uses CREATE SCHEMA IF NOT EXISTS.
     */
    @Transactional
    public void provisionSchema(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "Tenant not found: " + tenantId));

        String schemaName = tenant.getSchemaName();
        log.info("Provisioning schema: schema={} tenant={}", schemaName, tenant.getTenantCode());

        try (Connection conn = platformDataSource.getConnection()) {
            createSchema(conn, schemaName);
            useSchema(conn, schemaName);
            runMigrationScripts(conn, schemaName, tenant.getTenantCode());
        } catch (Exception e) {
            log.error("Schema provisioning failed: schema={} tenant={}", schemaName, tenant.getTenantCode(), e);
            throw new BusinessException("SCHEMA_PROVISION_FAILED",
                    "Failed to provision schema for tenant: " + tenant.getTenantCode());
        }

        log.info("Schema provisioned: schema={} tenant={}", schemaName, tenant.getTenantCode());
    }

    private void createSchema(Connection conn, String schemaName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS `" + schemaName + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private void useSchema(Connection conn, String schemaName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("USE `" + schemaName + "`");
        }
    }

    private void runMigrationScripts(Connection conn, String schemaName, String tenantCode) throws Exception {
        for (String scriptPath : MIGRATION_SCRIPTS) {
            log.debug("Running migration: script={} schema={}", scriptPath, schemaName);
            String sql = loadScript(scriptPath);
            executeScript(conn, sql, scriptPath);
        }
        log.info("All migration scripts applied: schema={} tenant={}", schemaName, tenantCode);
    }

    private String loadScript(String classPathLocation) throws Exception {
        ClassPathResource resource = new ClassPathResource(classPathLocation);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    private void executeScript(Connection conn, String sql, String scriptPath) throws SQLException {
        // Split on semicolons, skip blank/comment-only segments
        String[] statements = sql.split(";");
        try (Statement stmt = conn.createStatement()) {
            for (String raw : statements) {
                String trimmed = raw.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                stmt.execute(trimmed);
            }
        } catch (SQLException e) {
            throw new SQLException("Migration script failed: " + scriptPath + " — " + e.getMessage(), e);
        }
    }
}
