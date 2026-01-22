package com.mytegroup.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Application context loading test.
 * Uses TestcontainersSetup for database container management.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // Verify TestcontainersSetup is working
        assertThat(TestcontainersSetup.isRunning()).isTrue();
    }
}

