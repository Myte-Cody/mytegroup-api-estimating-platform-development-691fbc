package com.mytegroup.api.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test-only Flyway configuration that forces migrations to run
 * against the Testcontainers-managed datasource.
 */
@TestConfiguration
public class TestFlywayConfig {

    @Bean
    @Primary
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .load();
    }

    @Bean
    public ApplicationRunner flywayRunner(Flyway flyway) {
        return args -> flyway.migrate();
    }
}
