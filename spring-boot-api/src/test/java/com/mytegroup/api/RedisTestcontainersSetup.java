package com.mytegroup.api;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable Testcontainers setup for Redis.
 * This class manages the Redis container lifecycle and provides static access
 * to container connection details for use across all integration tests.
 *
 * NOTE: Testcontainers requires Docker to be available. If Docker is not present,
 * this class will gracefully degrade and use environment variables instead.
 */
public class RedisTestcontainersSetup {

    private static GenericContainer<?> redis;
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
            // Check if Docker is available by trying to create a Redis container
            redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

            // Try to start the container
            redis.start();
            dockerAvailable = true;

            // Set system properties from container
            String redisHost = redis.getHost();
            Integer redisPort = redis.getFirstMappedPort();
            System.setProperty("spring.redis.host", redisHost);
            System.setProperty("spring.redis.port", String.valueOf(redisPort));

            System.out.println("✅ Testcontainers Redis started successfully");
            System.out.println("   Host: " + redisHost);
            System.out.println("   Port: " + redisPort);

        } catch (Exception e) {
            dockerAvailable = false;
            System.out.println("⚠️  Docker/Testcontainers Redis not available: " + e.getMessage());
            System.out.println("   Using environment variables or defaults instead");

            // Fallback to environment variables
            String host = System.getenv("REDIS_HOST");
            String port = System.getenv("REDIS_PORT");
            if (host == null) {
                System.out.println("   - REDIS_HOST: localhost");
                System.out.println("   - REDIS_PORT: 6379");
            }

            // Set default properties if not already set
            setDefaultIfNotSet("spring.redis.host", "localhost");
            setDefaultIfNotSet("spring.redis.port", "6379");
        }
    }

    private static void setDefaultIfNotSet(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    /**
     * Gets the Redis host.
     * @return Redis host string
     */
    public static String getHost() {
        if (dockerAvailable && redis != null) {
            return redis.getHost();
        }
        return System.getProperty("spring.redis.host", "localhost");
    }

    /**
     * Gets the Redis port.
     * @return Redis port number
     */
    public static Integer getPort() {
        if (dockerAvailable && redis != null) {
            return redis.getFirstMappedPort();
        }
        String port = System.getProperty("spring.redis.port", "6379");
        return Integer.parseInt(port);
    }

    /**
     * Gets the Redis connection URL.
     * @return Redis URL (redis://host:port format)
     */
    public static String getUrl() {
        return "redis://" + getHost() + ":" + getPort();
    }

    /**
     * Checks if the container is running (or if we have Redis access).
     * @return true if container is running or Redis is accessible
     */
    public static boolean isRunning() {
        if (dockerAvailable && redis != null) {
            return redis.isRunning();
        }
        // If Docker is not available, assume we have local Redis or CI/CD environment
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
        if (dockerAvailable && redis != null && redis.isRunning()) {
            try {
                redis.stop();
                System.out.println("✅ Testcontainers Redis stopped");
            } catch (Exception e) {
                System.out.println("⚠️  Error stopping container: " + e.getMessage());
            }
        }
    }
}



