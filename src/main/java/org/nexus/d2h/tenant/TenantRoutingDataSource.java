package org.nexus.d2h.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Routes each request to the correct tenant schema by executing
 * {@code USE d2h_tenant_{tenantCode}} on the borrowed connection.
 * Falls back to the platform datasource when no tenant context is set
 * (PLATFORM_ADMIN requests, unauthenticated requests, login endpoint).
 */
@Slf4j
public class TenantRoutingDataSource extends AbstractDataSource {

    private static final String SCHEMA_PREFIX = "d2h_tenant_";

    private final DataSource delegate;
    private final boolean schemaRoutingEnabled;

    public TenantRoutingDataSource(DataSource delegate, boolean schemaRoutingEnabled) {
        this.delegate = delegate;
        this.schemaRoutingEnabled = schemaRoutingEnabled;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return switchSchema(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return switchSchema(delegate.getConnection(username, password));
    }

    private Connection switchSchema(Connection connection) throws SQLException {
        String tenantCode = TenantContext.getCurrentTenant();
        if (!schemaRoutingEnabled || tenantCode == null || tenantCode.isBlank()) {
            return connection;
        }
        String schema = SCHEMA_PREFIX + tenantCode;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("USE `" + schema + "`");
            log.debug("Switched to schema: {}", schema);
        } catch (SQLException e) {
            connection.close();
            throw new SQLException("Failed to switch to tenant schema: " + schema, e);
        }
        return connection;
    }
}
