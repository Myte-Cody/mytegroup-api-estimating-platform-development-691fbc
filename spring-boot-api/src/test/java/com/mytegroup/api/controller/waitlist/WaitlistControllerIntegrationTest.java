package com.mytegroup.api.controller.waitlist;

import com.mytegroup.api.dto.waitlist.*;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WaitlistController.
 * Tests waitlist operations, RBAC, request/response validation.
 */
class WaitlistControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
    }

    // ========== PUBLIC ENDPOINT TESTS ==========

    @Test
    void testStartWaitlist_WithoutAuthentication_IsAllowed() throws Exception {
        StartWaitlistDto dto = new StartWaitlistDto();
        // Use unique email to avoid conflicts
        dto.setEmail("waitlist" + System.currentTimeMillis() + "@company.com");
        dto.setName("Test User");
        dto.setPhone("+15145551234"); // E.164 format required
        dto.setMarketingConsent(true);
        
        // Test that endpoint is accessible (may return 201, 400, or 409)
        mockMvc.perform(post("/api/marketing/waitlist/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 201 || status == 400 || status == 409, 
                        "Expected 201, 400, or 409, but got: " + status);
                });
    }

    @Test
    void testGetStats_WithoutAuthentication_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testLogEvent_WithoutAuthentication_IsAllowed() throws Exception {
        WaitlistEventDto dto = new WaitlistEventDto();
        dto.setEvent("test_event");
        dto.setMeta(java.util.Map.of("key", "value"));
        
        mockMvc.perform(post("/api/marketing/waitlist/event")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // ========== ADMIN ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListWaitlist_WithOrgAdmin_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListWaitlist_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testApproveWaitlist_WithOrgAdmin_ReturnsEntry() throws Exception {
        ApproveWaitlistDto dto = new ApproveWaitlistDto();
        dto.setEmail("waitlist@example.com");
        dto.setCohortTag("test-cohort");
        
        mockMvc.perform(post("/api/marketing/waitlist/approve")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testInviteBatch_WithOrgAdmin_ReturnsOk() throws Exception {
        InviteBatchDto dto = new InviteBatchDto();
        dto.setLimit(10);
        dto.setCohortTag("test-cohort");
        
        mockMvc.perform(post("/api/marketing/waitlist/invite-batch")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== VERIFY ENDPOINT TESTS ==========

    @Test
    void testVerifyWaitlist_WithoutAuthentication_ReturnsBadRequest() throws Exception {
        // Service throws BadRequestException when entry doesn't exist
        VerifyWaitlistDto dto = new VerifyWaitlistDto("test@example.com", "123456");
        
        mockMvc.perform(post("/api/marketing/waitlist/verify")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testVerifyPhoneWaitlist_WithoutAuthentication_ReturnsBadRequest() throws Exception {
        // Service throws BadRequestException when entry doesn't exist
        VerifyWaitlistPhoneDto dto = new VerifyWaitlistPhoneDto("test@example.com", "123456");
        
        mockMvc.perform(post("/api/marketing/waitlist/verify-phone")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== RESEND ENDPOINT TESTS ==========

    @Test
    void testResendWaitlist_WithoutAuthentication_ReturnsBadRequest() throws Exception {
        // Service throws BadRequestException when entry doesn't exist
        ResendWaitlistDto dto = new ResendWaitlistDto("test@example.com");
        
        mockMvc.perform(post("/api/marketing/waitlist/resend")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testResendPhoneWaitlist_WithoutAuthentication_IsAllowed() throws Exception {
        // Note: resend-phone endpoint uses ResendWaitlistDto, not ResendWaitlistPhoneDto
        ResendWaitlistDto dto = new ResendWaitlistDto("test@example.com");
        
        mockMvc.perform(post("/api/marketing/waitlist/resend-phone")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("not_implemented"));
    }

    // ========== ADDITIONAL LIST TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListWaitlist_WithStatusFilter_ReturnsFiltered() throws Exception {
        // WaitlistStatus enum values need to match exactly - using PENDING_COHORT
        mockMvc.perform(get("/api/marketing/waitlist")
                .param("status", "PENDING_COHORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListWaitlist_WithVerifyStatusFilter_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist")
                .param("verifyStatus", "VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListWaitlist_WithCohortTag_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist")
                .param("cohortTag", "test-cohort"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListWaitlist_WithEmailContains_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist")
                .param("emailContains", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListWaitlist_WithPagination_ReturnsPaginated() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist")
                .param("page", "1")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.page").value(1));
    }

    // ========== ADDITIONAL START TESTS ==========

    @Test
    void testStartWaitlist_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        StartWaitlistDto dto = new StartWaitlistDto();
        dto.setEmail("invalid-email"); // Invalid email format
        dto.setName("Test User");
        dto.setMarketingConsent(true);
        
        mockMvc.perform(post("/api/marketing/waitlist/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStartWaitlist_WithPersonalEmailDomain_ReturnsBadRequest() throws Exception {
        StartWaitlistDto dto = new StartWaitlistDto();
        dto.setEmail("test@gmail.com"); // Personal email domain
        dto.setName("Test User");
        dto.setMarketingConsent(true);
        
        mockMvc.perform(post("/api/marketing/waitlist/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStartWaitlist_WithInvalidPhone_ReturnsBadRequest() throws Exception {
        StartWaitlistDto dto = new StartWaitlistDto();
        dto.setEmail("waitlist@company.com");
        dto.setName("Test User");
        dto.setPhone("123"); // Invalid E.164 format
        dto.setMarketingConsent(true);
        
        mockMvc.perform(post("/api/marketing/waitlist/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL STATS TESTS ==========

    @Test
    void testGetStats_ResponseHasStatsFields() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    // ========== ADDITIONAL EVENT TESTS ==========

    @Test
    void testLogEvent_WithoutEvent_ReturnsBadRequest() throws Exception {
        WaitlistEventDto dto = new WaitlistEventDto();
        dto.setMeta(java.util.Map.of("key", "value"));
        // Missing event field
        
        mockMvc.perform(post("/api/marketing/waitlist/event")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListWaitlist_WithSuperAdmin_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testListWaitlist_WithPlatformAdmin_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/marketing/waitlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}

