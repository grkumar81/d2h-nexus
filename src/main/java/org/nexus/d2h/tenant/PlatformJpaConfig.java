package org.nexus.d2h.tenant;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(
        basePackages = {
                "org.nexus.d2h.tenant",
                "org.nexus.d2h.user"
        },
        entityManagerFactoryRef = "platformEntityManagerFactory",
        transactionManagerRef = "platformTransactionManager"
)
public class PlatformJpaConfig {

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.MySQLDialect}")
    private String dialect;

    @Primary
    @Bean(name = "platformEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean platformEntityManagerFactory(
            @Qualifier("platformDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
                "org.nexus.d2h.tenant",
                "org.nexus.d2h.user",
                "org.nexus.d2h.common"
        );
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaPropertyMap(jpaProperties());
        em.setPersistenceUnitName("platform");
        return em;
    }

    @Primary
    @Bean(name = "platformTransactionManager")
    public PlatformTransactionManager platformTransactionManager(
            @Qualifier("platformEntityManagerFactory") EntityManagerFactory emf) {
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
