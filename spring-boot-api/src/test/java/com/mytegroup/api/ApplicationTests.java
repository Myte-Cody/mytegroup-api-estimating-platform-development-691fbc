package com.mytegroup.api;

import com.mytegroup.api.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application context loading test.
 * Verifies that the Spring application context loads successfully with all beans.
 */
@SpringBootTest(classes = {
    Application.class
}, properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
    "spring.data.redis.repositories.enabled=false"
})
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the application context has loaded successfully
        // This verifies all beans (including mocked Redis) are properly configured
    }
}


