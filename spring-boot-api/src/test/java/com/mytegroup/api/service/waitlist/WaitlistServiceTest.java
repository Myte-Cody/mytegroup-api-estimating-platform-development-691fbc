package com.mytegroup.api.service.waitlist;

import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.core.WaitlistEntry;
import com.mytegroup.api.entity.enums.core.WaitlistStatus;
import com.mytegroup.api.entity.enums.core.WaitlistVerifyStatus;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.repository.core.WaitlistEntryRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.email.EmailService;
import com.mytegroup.api.service.users.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @Mock
    private UsersService usersService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WaitlistService waitlistService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(waitlistService, "inviteGateEnabled", false);
        ReflectionTestUtils.setField(waitlistService, "domainGateEnabled", false);
        ReflectionTestUtils.setField(waitlistService, "requireInviteToken", false);
        ReflectionTestUtils.setField(waitlistService, "verificationTtlMinutes", 30);
        ReflectionTestUtils.setField(waitlistService, "verificationCodeLength", 6);
        ReflectionTestUtils.setField(waitlistService, "maxAttempts", 5);
        ReflectionTestUtils.setField(waitlistService, "maxTotalAttempts", 12);
        ReflectionTestUtils.setField(waitlistService, "maxResends", 6);
        ReflectionTestUtils.setField(waitlistService, "blockMinutes", 60);
        ReflectionTestUtils.setField(waitlistService, "resendCooldownMinutes", 2);
        ReflectionTestUtils.setField(waitlistService, "clientBaseUrl", "http://localhost:4001");
    }

    @Test
    void testStart_WithInvalidEmail_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            waitlistService.start("invalid-email", "+15145551234", "Test User", "user", "web", false, false);
        });
    }

    @Test
    void testStart_WithInvalidPhone_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            waitlistService.start("test@example.com", "invalid-phone", "Test User", "user", "web", false, false);
        });
    }

    @Test
    void testStart_WithPersonalEmailDomain_ThrowsForbiddenException() {
        assertThrows(ForbiddenException.class, () -> {
            waitlistService.start("test@gmail.com", "+15145551234", "Test User", "user", "web", false, false);
        });
    }

    @Test
    void testStart_WithActiveUser_ReturnsOk() {
        String email = "test@example.com";
        User activeUser = new User();
        activeUser.setEmail(email);
        when(usersService.findByEmail(anyString())).thenReturn(activeUser);
        lenient().doNothing().when(auditLogService).log(eq("marketing.event"), isNull(), isNull(), isNull(), isNull(), anyMap());

        Map<String, Object> result = waitlistService.start(email, "+15145551234", "Test User", "user", "web", false, false);

        assertNotNull(result);
        assertEquals("ok", result.get("status"));
        assertNull(result.get("entry"));
        verify(waitlistEntryRepository, never()).save(any());
    }

    @Test
    void testStart_WithNewEntry_CreatesEntry() {
        String email = "test@example.com";
        String phone = "+15145551234";
        when(usersService.findByEmail(email)).thenReturn(null);
        when(waitlistEntryRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(waitlistEntryRepository.save(any(WaitlistEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        doNothing().when(emailService).sendWaitlistVerificationEmail(anyString(), anyString(), anyString());

        Map<String, Object> result = waitlistService.start(email, phone, "Test User", "user", "web", false, false);

        assertNotNull(result);
        verify(waitlistEntryRepository, times(1)).save(any(WaitlistEntry.class));
        verify(emailService, atMost(1)).sendWaitlistVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testStart_WithExistingEntry_UpdatesEntry() {
        String email = "test@example.com";
        String phone = "+15145551234";
        WaitlistEntry existingEntry = new WaitlistEntry();
        existingEntry.setEmail(email);
        existingEntry.setName("Test User");
        existingEntry.setPhone(phone);
        existingEntry.setRole("user");
        existingEntry.setVerifyStatus(WaitlistVerifyStatus.UNVERIFIED);
        existingEntry.setPhoneVerifyStatus(WaitlistVerifyStatus.UNVERIFIED);
        existingEntry.setVerifyAttempts(0);
        existingEntry.setVerifyAttemptTotal(0);
        existingEntry.setVerifyResends(0);
        existingEntry.setPhoneVerifyAttempts(0);
        existingEntry.setPhoneVerifyAttemptTotal(0);
        existingEntry.setPhoneVerifyResends(0);

        when(usersService.findByEmail(email)).thenReturn(null);
        when(waitlistEntryRepository.findByEmail(email)).thenReturn(Optional.of(existingEntry));
        when(waitlistEntryRepository.save(any(WaitlistEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        doNothing().when(emailService).sendWaitlistVerificationEmail(anyString(), anyString(), anyString());

        Map<String, Object> result = waitlistService.start(email, phone, "Test User", "user", "web", false, false);

        assertNotNull(result);
        verify(waitlistEntryRepository, times(1)).save(any(WaitlistEntry.class));
    }

    @Test
    void testStart_WithAlreadyVerifiedEmail_SkipsEmailVerification() {
        String email = "test@example.com";
        String phone = "+15145551234";
        WaitlistEntry existingEntry = new WaitlistEntry();
        existingEntry.setEmail(email);
        existingEntry.setName("Test User");
        existingEntry.setPhone(phone);
        existingEntry.setRole("user");
        existingEntry.setVerifyStatus(WaitlistVerifyStatus.VERIFIED);
        existingEntry.setPhoneVerifyStatus(WaitlistVerifyStatus.UNVERIFIED);
        existingEntry.setVerifyAttempts(0);
        existingEntry.setVerifyAttemptTotal(0);
        existingEntry.setVerifyResends(0);
        existingEntry.setPhoneVerifyAttempts(0);
        existingEntry.setPhoneVerifyAttemptTotal(0);
        existingEntry.setPhoneVerifyResends(0);

        when(usersService.findByEmail(email)).thenReturn(null);
        when(waitlistEntryRepository.findByEmail(email)).thenReturn(Optional.of(existingEntry));
        when(waitlistEntryRepository.save(any(WaitlistEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Map<String, Object> result = waitlistService.start(email, phone, "Test User", "user", "web", false, false);

        assertNotNull(result);
        verify(emailService, never()).sendWaitlistVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testShouldEnforceInviteGate_ReturnsCorrectValue() {
        ReflectionTestUtils.setField(waitlistService, "inviteGateEnabled", true);
        assertTrue(waitlistService.shouldEnforceInviteGate());

        ReflectionTestUtils.setField(waitlistService, "inviteGateEnabled", false);
        assertFalse(waitlistService.shouldEnforceInviteGate());
    }

    @Test
    void testRequiresInviteToken_ReturnsCorrectValue() {
        ReflectionTestUtils.setField(waitlistService, "requireInviteToken", true);
        assertTrue(waitlistService.requiresInviteToken());

        ReflectionTestUtils.setField(waitlistService, "requireInviteToken", false);
        assertFalse(waitlistService.requiresInviteToken());
    }

    @Test
    void testDomainGateEnabled_ReturnsCorrectValue() {
        ReflectionTestUtils.setField(waitlistService, "domainGateEnabled", true);
        assertTrue(waitlistService.domainGateEnabled());

        ReflectionTestUtils.setField(waitlistService, "domainGateEnabled", false);
        assertFalse(waitlistService.domainGateEnabled());
    }

    @Test
    void testAcquireDomainClaim_WithRedis_ReturnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        boolean result = waitlistService.acquireDomainClaim("example.com");

        assertTrue(result);
        verify(valueOperations, times(1)).setIfAbsent(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testReleaseDomainClaim_WithRedis_DeletesKey() {
        waitlistService.releaseDomainClaim("example.com");

        verify(redisTemplate, times(1)).delete(anyString());
    }
}

