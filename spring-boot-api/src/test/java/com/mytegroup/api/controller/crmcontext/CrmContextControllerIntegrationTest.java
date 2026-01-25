package com.mytegroup.api.controller.crmcontext;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CrmContextController.
 * Tests CRM context operations, RBAC, request/response validation.
 */
class CrmContextControllerIntegrationTest extends BaseControllerTest {

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
    void testListDocuments_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/crm-context/documents")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListDocuments_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/crm-context/documents")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST DOCUMENTS ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListDocuments_WithoutOrgId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/crm-context/documents"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("orgId is required"));
    }

    // ========== INDEX DOCUMENT ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testIndexDocument_WithOrgAdmin_ReturnsNotImplemented() throws Exception {
        // This endpoint throws UnsupportedOperationException
        mockMvc.perform(post("/api/crm-context/documents/doc-123/index")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testIndexDocument_WithoutOrgId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/crm-context/documents/doc-123/index")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("orgId is required"));
    }
}

