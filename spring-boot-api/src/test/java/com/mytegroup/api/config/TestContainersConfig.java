package com.mytegroup.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 * The ApplicationTests class handles dynamic property configuration.
 */
@TestConfiguration
public class TestContainersConfig {

    @Bean
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
                .withDatabaseName("mytegroup_test")
                .withUsername("postgres")
                .withPassword("postgres")
                .withReuse(true);
        container.start();
        return container;
    }
}

