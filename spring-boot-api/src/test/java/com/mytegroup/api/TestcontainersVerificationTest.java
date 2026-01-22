package com.mytegroup.api;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Standalone test to verify Testcontainers setup works independently.
 * This test doesn't require Spring Boot context, just verifies the container can start.
 */
@Testcontainers
class TestcontainersVerificationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("testcontainers_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void shouldStartPostgreSQLContainer() {
        // Verify container is running
        assertThat(postgres.isRunning()).isTrue();
        
        // Verify we can connect to the database
        String jdbcUrl = postgres.getJdbcUrl();
        assertThat(jdbcUrl).isNotNull();
        assertThat(jdbcUrl).contains("postgresql");
        
        // Try to execute a simple query
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT version()")) {
                    assertThat(resultSet.next()).isTrue();
                    String version = resultSet.getString(1);
                    assertThat(version).isNotNull();
                    assertThat(version).contains("PostgreSQL");
                    System.out.println("PostgreSQL Version: " + version);
                }
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to connect to PostgreSQL container", e);
        }
    }

    @Test
    void shouldVerifyTestcontainersSetupClass() {
        // Verify the TestcontainersSetup class can be accessed
        // Note: This test doesn't actually start TestcontainersSetup's container
        // but verifies the class is available
        assertThat(TestcontainersSetup.class).isNotNull();
    }
}

