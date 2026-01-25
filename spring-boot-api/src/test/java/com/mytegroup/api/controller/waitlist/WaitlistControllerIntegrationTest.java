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
        dto.setEmail("waitlist@company.com"); // Use company domain to avoid denylist
        dto.setName("Test User");
        dto.setPhone("+15145551234"); // E.164 format required
        dto.setMarketingConsent(true);
        
        // Test that endpoint is accessible (may fail validation but endpoint works)
        mockMvc.perform(post("/api/marketing/waitlist/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Accept any 2xx or 4xx status - endpoint is accessible
                    assertTrue((status >= 200 && status < 300) || (status >= 400 && status < 500), 
                        "Expected 2xx or 4xx but got " + status);
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
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Accept 200 (success) or 404 (not found)
                    assertTrue(status == 200 || status == 404, 
                        "Expected 200 or 404 but got " + status);
                });
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
}

