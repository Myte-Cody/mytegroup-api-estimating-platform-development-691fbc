package com.mytegroup.api.service.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    @Test
    void testSendSms_WithValidParams_ReturnsStatus() {
        String to = "+15145551234";
        String body = "Test message";

        Map<String, String> result = smsService.sendSms(to, body);

        assertNotNull(result);
        assertEquals("stubbed", result.get("status"));
    }

    @Test
    void testSendVerificationCode_WithValidParams_ReturnsStatus() {
        String phone = "+15145551234";
        String code = "123456";

        Map<String, String> result = smsService.sendVerificationCode(phone, code);

        assertNotNull(result);
        assertEquals("stubbed", result.get("status"));
    }
}


