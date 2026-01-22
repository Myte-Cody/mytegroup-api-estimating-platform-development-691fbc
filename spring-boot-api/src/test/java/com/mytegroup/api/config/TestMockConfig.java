package com.mytegroup.api.config;

import com.mytegroup.api.service.sms.SmsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Test configuration for mocking external integrations.
 * This configuration mocks external services that should not be called during tests.
 */
@TestConfiguration
public class TestMockConfig {

    /**
     * Mock JavaMailSender to prevent actual email sending during tests.
     */
    @MockBean
    private JavaMailSender javaMailSender;

    /**
     * Mock SmsService to prevent actual SMS sending during tests.
     */
    @MockBean
    private SmsService smsService;
}

