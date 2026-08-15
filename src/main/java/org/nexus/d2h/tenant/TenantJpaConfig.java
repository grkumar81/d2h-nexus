package org.nexus.d2h.tenant;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = {
                "org.nexus.d2h.retailer",
                "org.nexus.d2h.asset",
                "org.nexus.d2h.boxsale",
                "org.nexus.d2h.finance",
                "org.nexus.d2h.recharge",
                "org.nexus.d2h.notification",
                "org.nexus.d2h.audit",
                "org.nexus.d2h.upload"
        },
        entityManagerFactoryRef = "tenantEntityManagerFactory",
        transactionManagerRef = "tenantTransactionManager"
)
public class TenantJpaConfig {

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.MySQLDialect}")
    private String dialect;

    @Bean(name = "tenantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            @Qualifier("tenantDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
                "org.nexus.d2h.retailer",
                "org.nexus.d2h.asset",
                "org.nexus.d2h.boxsale",
                "org.nexus.d2h.finance",
                "org.nexus.d2h.recharge",
                "org.nexus.d2h.notification",
                "org.nexus.d2h.audit",
                "org.nexus.d2h.upload"
        );
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaPropertyMap(jpaProperties());
        em.setPersistenceUnitName("tenant");
        return em;
    }

    @Bean(name = "tenantTransactionManager")
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "false");
        props.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "1000");
        return props;
    }
}
