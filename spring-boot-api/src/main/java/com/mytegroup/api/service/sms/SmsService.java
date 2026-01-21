package com.mytegroup.api.service.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for SMS sending.
 * Handles SMS sending via Twilio or stub implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {
    
    /**
     * Sends an SMS message
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Map<String, String> sendSms(String to, String body) {
        // TODO: Implement Twilio SMS sending
        // For now, stub implementation
        log.debug("[sms] stubbed send to={}", to);
        return Map.of("status", "stubbed");
    }
    
    /**
     * Sends a verification code via SMS
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Map<String, String> sendVerificationCode(String phone, String code) {
        String message = "Your verification code is: " + code;
        return sendSms(phone, message);
    }
}

