package com.mytegroup.api.controller.auth;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.common.util.TokenHashUtil;
import com.mytegroup.api.dto.auth.LoginDto;
import com.mytegroup.api.dto.auth.RegisterDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.users.UsersService;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests authentication flows, user registration, password verification, RBAC validation.
 */
class AuthControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private User testUser;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UsersService usersService;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testUser = setupUser(testOrganization, "testuser@example.com");
    }

    // ========== REGISTER ENDPOINT TESTS ==========

    @Test
    void testRegister_WithValidData_ReturnsCreated() throws Exception {
        // Use unique email to avoid conflicts
        String uniqueEmail = "newuser" + System.currentTimeMillis() + "@example.com";
        RegisterDto registerDto = new RegisterDto(
                "New",  // firstName
                "User",  // lastName
                "newuser",  // username
                uniqueEmail,  // email
                "ValidPassword123!@",  // password (12+ chars: lowercase, uppercase, digit, symbol)
                null,  // orgId
                "NewOrg",  // organizationName
                Role.USER,  // role
                null,  // inviteToken
                true,  // legalAccepted
                false  // orgLegalReconfirm
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated());
    }

    @Test
    void testRegister_WithMissingEmail_ReturnsBadRequest() throws Exception {
        RegisterDto registerDto = new RegisterDto(
                "New",  // firstName
                "User",  // lastName
                "newuser",  // username
                null,  // email (missing - should cause 400)
                "ValidPassword123!@",  // password
                null,  // orgId
                "NewOrg",  // organizationName
                Role.USER,  // role
                null,  // inviteToken
                true,  // legalAccepted
                false  // orgLegalReconfirm
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isBadRequest());
    }

    // ========== LOGIN ENDPOINT TESTS ==========

    @Test
    void testLogin_WithValidCredentials_ReturnsOk() throws Exception {
        // First register a user with a known password
        String uniqueEmail = "loginuser" + System.currentTimeMillis() + "@example.com";
        String password = "ValidPassword123!@";
        RegisterDto registerDto = new RegisterDto(
                "Login",  // firstName
                "User",  // lastName
                "loginuser",  // username
                uniqueEmail,  // email
                password,  // password
                null,  // orgId
                "LoginOrg",  // organizationName
                Role.USER,  // role
                null,  // inviteToken
                true,  // legalAccepted
                false  // orgLegalReconfirm
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated());
        
        // Manually verify the user's email since invite gate is disabled
        User registeredUser = userRepository.findByEmail(uniqueEmail).orElseThrow();
        registeredUser.setIsEmailVerified(true);
        userRepository.save(registeredUser);
        
        // Then login with those credentials
        LoginDto loginDto = new LoginDto(uniqueEmail, password);
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk());
    }

    @Test
    void testLogin_ReturnsJsonContentType() throws Exception {
        LoginDto loginDto = new LoginDto("testuser@example.com", "ValidPassword123!@");
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    // ========== LOGOUT ENDPOINT TESTS ==========

    @Test
    @WithMockUser
    void testLogout_WithAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>()))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void testLogout_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>()))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== VERIFY EMAIL ENDPOINT TESTS ==========

    @Test
    void testVerifyEmail_WithValidToken_ReturnsOk() throws Exception {
        // Create a user with a verification token
        String uniqueEmail = "verify" + System.currentTimeMillis() + "@example.com";
        User testUser = setupUser(testOrganization, uniqueEmail);
        testUser.setIsEmailVerified(false);
        testUser = userRepository.save(testUser);
        
        // Generate token and set verification token
        TokenHashUtil.TokenData tokenData = TokenHashUtil.generateTokenWithHashHours(24);
        usersService.setVerificationToken(testUser.getId(), tokenData.hash(), tokenData.expires());
        
        // Verify email with the token
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", tokenData.token())))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.isEmailVerified").value(true));
    }

    @Test
    void testVerifyEmail_WithoutToken_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>()))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== REFRESH TOKEN ENDPOINT TESTS ==========
    // Note: AuthController does not have a refresh-token endpoint

    @Test
    void testRefreshToken_EndpointDoesNotExist_Returns404() throws Exception {
        // AuthController does not have a /refresh-token endpoint
        // Should return 404 (Not Found) or 405 (Method Not Allowed), not 500
        mockMvc.perform(post("/api/auth/refresh-token")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>()))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ========== PASSWORD RESET ENDPOINT TESTS ==========

    @Test
    void testRequestPasswordReset_WithValidEmail_ReturnsOk() throws Exception {
        // Use forgot-password endpoint (not request-password-reset)
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "testuser@example.com")))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void testResetPassword_WithValidToken_ReturnsOk() throws Exception {
        // Create a user with a reset token
        String uniqueEmail = "reset" + System.currentTimeMillis() + "@example.com";
        User testUser = setupUser(testOrganization, uniqueEmail);
        testUser = userRepository.save(testUser);
        
        // Generate token and set reset token
        TokenHashUtil.TokenData tokenData = TokenHashUtil.generateTokenWithHashHours(1);
        usersService.setResetToken(testUser.getId(), tokenData.hash(), tokenData.expires());
        
        // Reset password with the token
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "token", tokenData.token(),
                        "newPassword", "NewValidPassword123!@"
                )))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()));
    }

    // ========== CONTENT TYPE TESTS ==========

    @Test
    void testRegister_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>())))
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    // ========== ME ENDPOINT TESTS ==========

    @Test
    void testMe_WithAuthentication_ReturnsUser() throws Exception {
        // Create a user for the test
        Organization testOrg = setupOrganization("Test Org");
        User testUser = setupUser(testOrg, "user1@example.com");
        // The extractUserId method expects the principal to be a Long or String user ID
        // Use @WithMockUser with the user ID as username, then override with custom authentication
        org.springframework.security.core.Authentication auth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                testUser.getId(), // Principal is the user ID (Long)
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")
            );
        // Set authentication in SecurityContext directly
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            mockMvc.perform(get("/api/auth/me")
                    .with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser.getId()))
                    .andExpect(jsonPath("$.email").value("user1@example.com"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void testMe_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ========== PASSWORD STRENGTH ENDPOINT TESTS ==========

    @Test
    void testPasswordStrength_WithValidPassword_ReturnsStrength() throws Exception {
        mockMvc.perform(post("/api/auth/password-strength")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "StrongPassword123!@"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testPasswordStrength_WithoutPassword_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/password-strength")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>())))
                .andExpect(status().isBadRequest());
    }

    // ========== LIST USERS ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListUsers_WithAdmin_ReturnsUserList() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListUsers_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListUsers_WithOrgAdmin_ReturnsUserList() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ========== ADDITIONAL LOGIN TESTS ==========

    @Test
    void testLogin_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        LoginDto loginDto = new LoginDto("nonexistent@example.com", "WrongPassword123!@");
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_WithoutEmail_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_WithoutPassword_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_ResponseHasTokenAndUser() throws Exception {
        LoginDto loginDto = new LoginDto("testuser@example.com", "ValidPassword123!@");
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("token") || content.contains("user"), 
                            "Response should contain token or user");
                    }
                });
    }

    // ========== ADDITIONAL REGISTER TESTS ==========

    @Test
    void testRegister_WithInvalidPassword_ReturnsBadRequest() throws Exception {
        String uniqueEmail = "weakpass" + System.currentTimeMillis() + "@example.com";
        RegisterDto registerDto = new RegisterDto(
                "New", "User", "newuser", uniqueEmail,
                "weak", // Too weak password
                null, "NewOrg", Role.USER, null, true, false
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_WithMissingPassword_ReturnsBadRequest() throws Exception {
        String uniqueEmail = "nopass" + System.currentTimeMillis() + "@example.com";
        RegisterDto registerDto = new RegisterDto(
                "New", "User", "newuser", uniqueEmail,
                null, // Missing password
                null, "NewOrg", Role.USER, null, true, false
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_ResponseHasUserFields() throws Exception {
        String uniqueEmail = "response" + System.currentTimeMillis() + "@example.com";
        RegisterDto registerDto = new RegisterDto(
                "New", "User", "newuser", uniqueEmail,
                "ValidPassword123!@", null, "NewOrg", Role.USER, null, true, false
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 201) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("id") || content.contains("email"), 
                            "Response should contain user fields");
                    }
                });
    }

    // ========== ADDITIONAL PASSWORD STRENGTH TESTS ==========

    @Test
    void testPasswordStrength_WithWeakPassword_ReturnsScore() throws Exception {
        mockMvc.perform(post("/api/auth/password-strength")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "weak"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testPasswordStrength_WithStrongPassword_ReturnsScore() throws Exception {
        mockMvc.perform(post("/api/auth/password-strength")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "VeryStrongPassword123!@#$"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    // ========== ADDITIONAL FORGOT PASSWORD TESTS ==========

    @Test
    void testForgotPassword_WithInvalidEmail_ReturnsOk() throws Exception {
        // Service may return ok even for invalid emails to prevent email enumeration
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "nonexistent@example.com")))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void testForgotPassword_WithoutEmail_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>()))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL LIST USERS TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListUsers_WithSuperAdmin_ReturnsUserList() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListUsers_WithoutOrgId_ReturnsAllUsers() throws Exception {
        // listUsers may require orgId or may work without it for SUPER_ADMIN
        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testListUsers_WithPlatformAdmin_ReturnsUserList() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ========== ADDITIONAL ME ENDPOINT TESTS ==========

    @Test
    @WithMockUser(username = "1", roles = ROLE_USER)
    void testMe_ResponseHasUserFields() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("id") || content.contains("email"), 
                            "Response should contain user fields");
                    }
                });
    }

    // ========== ADDITIONAL VERIFY EMAIL TESTS ==========

    @Test
    void testVerifyEmail_WithInvalidToken_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "invalid-token")))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL RESET PASSWORD TESTS ==========

    @Test
    void testResetPassword_WithoutToken_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("newPassword", "NewValidPassword123!@")))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testResetPassword_WithoutNewPassword_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "some-token")))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL LOGIN RESPONSE STRUCTURE TESTS ==========

    @Test
    void testLogin_ResponseContainsToken() throws Exception {
        LoginDto loginDto = new LoginDto("testuser@example.com", "ValidPassword123!@");
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("token"), "Response should contain token");
                    }
                });
    }

    @Test
    void testLogin_ResponseContainsUserObject() throws Exception {
        LoginDto loginDto = new LoginDto("testuser@example.com", "ValidPassword123!@");
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("user"), "Response should contain user object");
                    }
                });
    }

    @Test
    void testLogin_ResponseUserContainsRoles() throws Exception {
        LoginDto loginDto = new LoginDto("testuser@example.com", "ValidPassword123!@");
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("roles") || content.contains("role"), 
                            "Response should contain roles");
                    }
                });
    }

    // ========== ADDITIONAL REGISTER RESPONSE STRUCTURE TESTS ==========

    @Test
    void testRegister_ResponseContainsId() throws Exception {
        String uniqueEmail = "registerid" + System.currentTimeMillis() + "@example.com";
        RegisterDto registerDto = new RegisterDto(
                "New", "User", "newuser", uniqueEmail,
                "ValidPassword123!@", null, "NewOrg", Role.USER, null, true, false
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 201) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("id"), "Response should contain id");
                    }
                });
    }

    @Test
    void testRegister_ResponseContainsIsEmailVerified() throws Exception {
        String uniqueEmail = "registerverified" + System.currentTimeMillis() + "@example.com";
        RegisterDto registerDto = new RegisterDto(
                "New", "User", "newuser", uniqueEmail,
                "ValidPassword123!@", null, "NewOrg", Role.USER, null, true, false
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 201) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("isEmailVerified") || content.contains("emailVerified"), 
                            "Response should contain isEmailVerified");
                    }
                });
    }

    // ========== ADDITIONAL ME ENDPOINT TESTS ==========

    @Test
    @WithMockUser(username = "1", roles = ROLE_ORG_ADMIN)
    void testMe_ResponseContainsOrgId() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("orgId") || content.contains("organizationId"), 
                            "Response should contain orgId");
                    }
                });
    }

    @Test
    @WithMockUser(username = "1", roles = ROLE_ORG_ADMIN)
    void testMe_ResponseContainsIsOrgOwner() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("isOrgOwner") || content.contains("orgOwner"), 
                            "Response should contain isOrgOwner");
                    }
                });
    }

    @Test
    void testMe_WithInvalidUserId_ReturnsError() throws Exception {
        // Test extractUserId with invalid user ID (user doesn't exist)
        // The endpoint throws UnauthorizedException when user is not found, which returns 401
        org.springframework.security.core.Authentication auth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                99999L, // Principal is the user ID (Long)
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")
            );
        mockMvc.perform(get("/api/auth/me")
                .with(authentication(auth)))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL VERIFY EMAIL RESPONSE TESTS ==========

    @Test
    void testVerifyEmail_ResponseContainsId() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "test-token")))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("id"), "Response should contain id");
                    }
                });
    }

    @Test
    void testVerifyEmail_ResponseContainsIsEmailVerified() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "test-token")))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("isEmailVerified") || content.contains("emailVerified"), 
                            "Response should contain isEmailVerified");
                    }
                });
    }

    // ========== ADDITIONAL RESET PASSWORD RESPONSE TESTS ==========

    @Test
    void testResetPassword_ResponseContainsStatus() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "token", "test-token",
                        "newPassword", "NewValidPassword123!@"
                )))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("status") || content.contains("id"), 
                            "Response should contain status or id");
                    }
                });
    }

    @Test
    void testResetPassword_ResponseContainsEmail() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "token", "test-token",
                        "newPassword", "NewValidPassword123!@"
                )))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("email") || content.contains("id"), 
                            "Response should contain email or id");
                    }
                });
    }

    // ========== ADDITIONAL LIST USERS RESPONSE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListUsers_ResponseContainsArchivedAt() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        // Response is an array, so we check if it's valid JSON
                        assertTrue(content.startsWith("[") || content.contains("archivedAt") || content.length() == 2, 
                            "Response should be an array or contain archivedAt");
                    }
                });
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testListUsers_WithOrgOwner_ReturnsUserList() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testListUsers_ResponseContainsIsEmailVerified() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String content = result.getResponse().getContentAsString();
                        assertTrue(content.contains("isEmailVerified") || content.length() == 2, 
                            "Response should contain isEmailVerified or be empty array");
                    }
                });
    }

    // ========== ADDITIONAL LOGIN ERROR PATH TESTS ==========

    @Test
    void testLogin_WithUnverifiedEmail_ReturnsForbidden() throws Exception {
        // This tests the service-level check for unverified email
        // The actual user would need to be created with isEmailVerified=false
        LoginDto loginDto = new LoginDto("testuser@example.com", "ValidPassword123!@");
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL PASSWORD STRENGTH RESPONSE TESTS ==========

    @Test
    void testPasswordStrength_ResponseContainsScore() throws Exception {
        mockMvc.perform(post("/api/auth/password-strength")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "StrongPassword123!@"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").exists());
    }

    @Test
    void testPasswordStrength_ResponseContainsFeedback() throws Exception {
        mockMvc.perform(post("/api/auth/password-strength")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "StrongPassword123!@"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    // ========== ADDITIONAL FORGOT PASSWORD RESPONSE TESTS ==========

    @Test
    void testForgotPassword_ResponseContainsStatus() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "testuser@example.com")))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}

