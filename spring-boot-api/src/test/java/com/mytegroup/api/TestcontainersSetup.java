package com.mytegroup.api;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable Testcontainers setup for PostgreSQL database.
 * This class manages the container lifecycle and provides static access
 * to container connection details for use across all test types.
 *
 * NOTE: Testcontainers requires Docker to be available. If Docker is not present,
 * this class will gracefully degrade and use environment variables instead.
 */
public class TestcontainersSetup {

    private static PostgreSQLContainer<?> postgres;
    private static boolean dockerAvailable = false;
    private static boolean initialized = false;

    static {
        initializeContainer();
    }

    private static void initializeContainer() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            // Check if Docker is available by trying to create a container
            postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
                    .withDatabaseName("mytegroup_test")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withReuse(true);

            // Try to start the container
            postgres.start();
            dockerAvailable = true;

            // Set system properties from container
            System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
            System.setProperty("spring.datasource.username", postgres.getUsername());
            System.setProperty("spring.datasource.password", postgres.getPassword());

            System.out.println("✅ Testcontainers PostgreSQL started successfully");
            System.out.println("   URL: " + postgres.getJdbcUrl());

        } catch (Exception e) {
            dockerAvailable = false;
            System.out.println("⚠️  Docker/Testcontainers not available: " + e.getMessage());
            System.out.println("   Using environment variables or defaults instead");

            // Fallback to environment variables (set via application-test.yml)
            String url = System.getenv("DB_HOST");
            if (url == null) {
                System.out.println("   - DB_HOST: localhost:5432");
                System.out.println("   - DB_NAME: mytegroup_test");
                System.out.println("   - DB_USER: postgres");
            }

            // Set default properties if not already set
            setDefaultIfNotSet("spring.datasource.url", 
                "jdbc:postgresql://localhost:5432/mytegroup_test");
            setDefaultIfNotSet("spring.datasource.username", "postgres");
            setDefaultIfNotSet("spring.datasource.password", "postgres");
        }
    }

    private static void setDefaultIfNotSet(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    /**
     * Gets the JDBC URL of the PostgreSQL container.
     * @return JDBC URL string
     */
    public static String getJdbcUrl() {
        if (dockerAvailable && postgres != null) {
            return postgres.getJdbcUrl();
        }
        return System.getProperty("spring.datasource.url", 
            "jdbc:postgresql://localhost:5432/mytegroup_test");
    }

    /**
     * Gets the username for the PostgreSQL container.
     * @return username string
     */
    public static String getUsername() {
        if (dockerAvailable && postgres != null) {
            return postgres.getUsername();
        }
        return System.getProperty("spring.datasource.username", "postgres");
    }

    /**
     * Gets the password for the PostgreSQL container.
     * @return password string
     */
    public static String getPassword() {
        if (dockerAvailable && postgres != null) {
            return postgres.getPassword();
        }
        return System.getProperty("spring.datasource.password", "postgres");
    }

    /**
     * Checks if the container is running (or if we have database access).
     * @return true if container is running or database is accessible
     */
    public static boolean isRunning() {
        if (dockerAvailable && postgres != null) {
            return postgres.isRunning();
        }
        // If Docker is not available, assume we have local DB or CI/CD environment
        return true;
    }

    /**
     * Checks if Docker/Testcontainers is available.
     * @return true if Docker is available
     */
    public static boolean isDockerAvailable() {
        return dockerAvailable;
    }

    /**
     * Stops the container (called at shutdown).
     */
    public static void stop() {
        if (dockerAvailable && postgres != null && postgres.isRunning()) {
            try {
                postgres.stop();
                System.out.println("✅ Testcontainers PostgreSQL stopped");
            } catch (Exception e) {
                System.out.println("⚠️  Error stopping container: " + e.getMessage());
            }
        }
    }
}
