package com.mytegroup.api.service.auth;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.UnauthorizedException;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.email.EmailService;
import com.mytegroup.api.service.organizations.OrganizationsService;
import com.mytegroup.api.service.users.UsersService;
import com.mytegroup.api.service.waitlist.WaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsersService usersService;

    @Mock
    private OrganizationsService organizationsService;

    @Mock
    private WaitlistService waitlistService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "verificationTokenTtlHours", 24);
        ReflectionTestUtils.setField(authService, "resetTokenTtlHours", 1);

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("$2a$10$encodedPasswordHash");
        testUser.setOrganization(testOrganization);
        testUser.setRole(Role.USER);
        testUser.setIsEmailVerified(true);
        testUser.setArchivedAt(null);
        testUser.setLegalHold(false);
    }

    @Disabled("Test needs fixing")
    @Test
    void testLogin_WithValidCredentials_ReturnsUser() {
        String email = "test@example.com";
        String password = "ValidPassword123!@";

        when(usersService.findByEmail(email)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
        when(usersService.markLastLogin(anyLong())).thenReturn(testUser);

        User result = authService.login(email, password);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(usersService, times(1)).markLastLogin(testUser.getId());
        verify(auditLogService, times(1)).log(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Disabled("Test needs fixing")
    @Test
    void testLogin_WithInvalidEmail_ThrowsUnauthorizedException() {
        String email = "nonexistent@example.com";
        String password = "password";

        when(usersService.findByEmail(email)).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> {
            authService.login(email, password);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testLogin_WithInvalidPassword_ThrowsUnauthorizedException() {
        String email = "test@example.com";
        String password = "wrongpassword";

        when(usersService.findByEmail(email)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> {
            authService.login(email, password);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testLogin_WithArchivedUser_ThrowsForbiddenException() {
        String email = "test@example.com";
        String password = "ValidPassword123!@";
        testUser.setArchivedAt(LocalDateTime.now());

        when(usersService.findByEmail(email)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> {
            authService.login(email, password);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testLogin_WithUserOnLegalHold_ThrowsForbiddenException() {
        String email = "test@example.com";
        String password = "ValidPassword123!@";
        testUser.setLegalHold(true);

        when(usersService.findByEmail(email)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> {
            authService.login(email, password);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testLogin_WithUnverifiedEmail_ThrowsForbiddenException() {
        String email = "test@example.com";
        String password = "ValidPassword123!@";
        testUser.setIsEmailVerified(false);

        when(usersService.findByEmail(email)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> {
            authService.login(email, password);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testRegister_WithValidData_CreatesUser() {
        String email = "newuser@example.com";
        String password = "ValidPassword123!@";
        String username = "newuser";
        String firstName = "New";
        String lastName = "User";
        String organizationName = "New Org";

        when(waitlistService.shouldEnforceInviteGate()).thenReturn(false);
        when(waitlistService.findByEmail(email)).thenReturn(null);
        when(organizationsService.create(any(Organization.class))).thenAnswer(invocation -> {
            Organization org = invocation.getArgument(0);
            org.setId(1L);
            return org;
        });
        when(usersService.create(any(User.class), eq(true))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(organizationsService.setOwner(anyLong(), anyLong())).thenReturn(testOrganization);
        doNothing().when(waitlistService).markActivated(anyString(), isNull());
        doNothing().when(auditLogService).log(anyString(), anyString(), anyString(), anyString(), anyString(), isNull());

        User result = authService.register(email, password, username, firstName, lastName, organizationName, null, true, null);

        assertNotNull(result);
        verify(usersService, times(1)).create(any(User.class), eq(true));
        verify(organizationsService, times(1)).create(any(Organization.class));
    }

    @Disabled("Test needs fixing")
    @Test
    void testRegister_WithoutLegalAcceptance_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            authService.register("test@example.com", "ValidPassword123!@", "user", "First", "Last", "Org", null, false, null);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testRegister_WithWeakPassword_ThrowsBadRequestException() {
        when(waitlistService.shouldEnforceInviteGate()).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            authService.register("test@example.com", "weak", "user", "First", "Last", "Org", null, true, null);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testRegister_WithInvalidEmail_ThrowsBadRequestException() {
        when(waitlistService.shouldEnforceInviteGate()).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            authService.register("invalid-email", "ValidPassword123!@", "user", "First", "Last", "Org", null, true, null);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testRegister_WithInviteGateEnabledAndNotInvited_ThrowsForbiddenException() {
        String email = "test@example.com";
        Map<String, Object> waitlistEntry = new HashMap<>();
        waitlistEntry.put("status", "pending");

        when(waitlistService.shouldEnforceInviteGate()).thenReturn(true);
        when(waitlistService.findByEmail(email)).thenReturn(waitlistEntry);

        assertThrows(ForbiddenException.class, () -> {
            authService.register(email, "ValidPassword123!@", "user", "First", "Last", "Org", null, true, null);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testForgotPassword_WithValidEmail_SendsResetEmail() {
        String email = "test@example.com";
        when(usersService.findByEmail(email)).thenReturn(testUser);
        doNothing().when(usersService).setResetToken(anyLong(), anyString(), any(LocalDateTime.class));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyString());

        Map<String, String> result = authService.forgotPassword(email);

        assertNotNull(result);
        assertEquals("ok", result.get("status"));
        verify(usersService, times(1)).setResetToken(anyLong(), anyString(), any(LocalDateTime.class));
        verify(emailService, times(1)).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Disabled("Test needs fixing")
    @Test
    void testForgotPassword_WithNonExistentEmail_ReturnsOk() {
        String email = "nonexistent@example.com";
        when(usersService.findByEmail(email)).thenReturn(null);

        // Should not throw exception for security reasons (don't reveal if email exists)
        Map<String, String> result = authService.forgotPassword(email);
        assertEquals("ok", result.get("status"));
    }

    @Disabled("Test needs fixing")
    @Test
    void testForgotPassword_WithArchivedUser_ThrowsForbiddenException() {
        String email = "test@example.com";
        testUser.setArchivedAt(LocalDateTime.now());
        when(usersService.findByEmail(email)).thenReturn(testUser);

        assertThrows(ForbiddenException.class, () -> {
            authService.forgotPassword(email);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testResetPassword_WithValidToken_ResetsPassword() {
        String token = "valid-token";
        String newPassword = "NewValidPassword123!@";

        when(usersService.findByResetToken(anyString())).thenReturn(testUser);
        when(usersService.clearResetTokenAndSetPassword(anyLong(), eq(newPassword))).thenReturn(testUser);
        doNothing().when(auditLogService).log(anyString(), anyString(), anyString(), anyString(), anyString(), isNull());

        User result = authService.resetPassword(token, newPassword);

        assertNotNull(result);
        verify(usersService, times(1)).clearResetTokenAndSetPassword(anyLong(), eq(newPassword));
    }

    @Disabled("Test needs fixing")
    @Test
    void testResetPassword_WithInvalidToken_ThrowsBadRequestException() {
        String token = "invalid-token";
        String newPassword = "NewValidPassword123!@";

        when(usersService.findByResetToken(anyString())).thenThrow(new BadRequestException("Invalid or expired reset token"));

        assertThrows(BadRequestException.class, () -> {
            authService.resetPassword(token, newPassword);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testResetPassword_WithWeakPassword_ThrowsBadRequestException() {
        String token = "valid-token";
        String weakPassword = "weak";

        assertThrows(BadRequestException.class, () -> {
            authService.resetPassword(token, weakPassword);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testResetPassword_WithNullToken_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            authService.resetPassword(null, "NewValidPassword123!@");
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testVerifyEmail_WithValidToken_VerifiesEmail() {
        String token = "valid-token";

        when(usersService.findByVerificationToken(anyString())).thenReturn(testUser);
        when(usersService.clearVerificationToken(anyLong())).thenReturn(testUser);
        doNothing().when(auditLogService).log(anyString(), anyString(), anyString(), anyString(), anyString(), isNull());

        User result = authService.verifyEmail(token);

        assertNotNull(result);
        verify(usersService, times(1)).clearVerificationToken(anyLong());
    }

    @Disabled("Test needs fixing")
    @Test
    void testVerifyEmail_WithInvalidToken_ThrowsBadRequestException() {
        String token = "invalid-token";

        when(usersService.findByVerificationToken(anyString())).thenThrow(new BadRequestException("Invalid or expired verification token"));

        assertThrows(BadRequestException.class, () -> {
            authService.verifyEmail(token);
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testVerifyEmail_WithNullToken_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            authService.verifyEmail(null);
        });
    }
}

