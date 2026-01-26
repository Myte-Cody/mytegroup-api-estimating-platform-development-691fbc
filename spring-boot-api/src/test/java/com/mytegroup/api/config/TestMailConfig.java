package com.mytegroup.api.config;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for GreenMail email testing.
 * Sets up an in-memory SMTP server for testing email functionality.
 */
@TestConfiguration
public class TestMailConfig {

    /**
     * Creates a GreenMail extension for JUnit 5.
     * GreenMail will start on port 1025 (SMTP) by default.
     */
    @Bean
    public GreenMailExtension greenMail() {
        return new GreenMailExtension(
            new ServerSetup(1025, null, ServerSetup.PROTOCOL_SMTP)
        )
        .withConfiguration(
            GreenMailConfiguration.aConfig()
                .withUser("test@localhost", "test")
        );
    }
}

