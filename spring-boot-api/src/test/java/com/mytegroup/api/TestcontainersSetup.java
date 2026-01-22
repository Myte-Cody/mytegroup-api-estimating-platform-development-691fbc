package com.mytegroup.api;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable Testcontainers setup for PostgreSQL database.
 * This class manages the container lifecycle and provides static access
 * to container connection details for use across all test types.
 */
@Testcontainers
public class TestcontainersSetup {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
            .withDatabaseName("mytegroup_test")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true);

    static {
        postgres.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }

    /**
     * Gets the JDBC URL of the PostgreSQL container.
     * @return JDBC URL string
     */
    public static String getJdbcUrl() {
        return postgres.getJdbcUrl();
    }

    /**
     * Gets the username for the PostgreSQL container.
     * @return username string
     */
    public static String getUsername() {
        return postgres.getUsername();
    }

    /**
     * Gets the password for the PostgreSQL container.
     * @return password string
     */
    public static String getPassword() {
        return postgres.getPassword();
    }

    /**
     * Checks if the container is running.
     * @return true if container is running
     */
    public static boolean isRunning() {
        return postgres.isRunning();
    }
}

