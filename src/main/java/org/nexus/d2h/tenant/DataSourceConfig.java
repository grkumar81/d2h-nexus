package org.nexus.d2h.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Two independent HikariCP pools:
 * <ul>
 *   <li>{@code platformDataSource} — dedicated pool always on {@code d2h_platform}.</li>
 *   <li>{@code tenantDataSource} — dedicated pool; TenantRoutingDataSource issues USE per request.</li>
 * </ul>
 * Pools are intentionally separate so a USE statement on a tenant connection
 * never contaminates a platform connection.
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:50}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.keepalive-time:60000}")
    private long keepaliveTime;

    @Value("${app.tenant.schema-routing-enabled:true}")
    private boolean schemaRoutingEnabled;

    @Primary
    @Bean(name = "platformDataSource", destroyMethod = "close")
    public HikariDataSource platformDataSource() {
        return buildPool("D2H-Platform-Pool", maxPoolSize, minIdle);
    }

    @Bean(name = "tenantDataSource")
    public DataSource tenantDataSource() {
        HikariDataSource pool = buildPool("D2H-Tenant-Pool", maxPoolSize, minIdle);
        return new TenantRoutingDataSource(pool, schemaRoutingEnabled);
    }

    private HikariDataSource buildPool(String poolName, int max, int idle) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(max);
        config.setMinimumIdle(idle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setKeepaliveTime(keepaliveTime);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName(poolName);
        return new HikariDataSource(config);
    }
}
