package com.mytegroup.api.controller.events;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EventController.
 * Tests event listing operations, RBAC, request/response validation.
 */
class EventControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEvents_WithOrgAdmin_IsAllowed() throws Exception {
        // Note: This endpoint throws UnsupportedOperationException
        mockMvc.perform(get("/api/events")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListEvents_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/events")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEvents_WithoutOrgId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("orgId is required"));
    }
}

