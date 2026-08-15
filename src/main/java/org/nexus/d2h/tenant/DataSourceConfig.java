package org.nexus.d2h.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Defines two datasources sharing one HikariCP connection pool:
 * <ul>
 *   <li>{@code platformDataSource} — connects to {@code d2h_platform}; used by platform JPA config.</li>
 *   <li>{@code tenantDataSource} — wraps the same pool; switches schema per request via USE statement.</li>
 * </ul>
 * Spring Boot's DataSource auto-configuration is excluded in favour of this explicit config.
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

    @Primary
    @Bean(name = "platformDataSource", destroyMethod = "close")
    public HikariDataSource platformDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setKeepaliveTime(keepaliveTime);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("D2H-Platform-Pool");
        return new HikariDataSource(config);
    }

    @Value("${app.tenant.schema-routing-enabled:true}")
    private boolean schemaRoutingEnabled;

    @Bean(name = "tenantDataSource")
    public DataSource tenantDataSource(@Qualifier("platformDataSource") DataSource platformDataSource) {
        return new TenantRoutingDataSource(platformDataSource, schemaRoutingEnabled);
    }
}
