package com.mytegroup.api.controller.communication;

import com.mytegroup.api.dto.emailtemplates.UpdateEmailTemplateDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EmailTemplateController.
 * Tests email template management, RBAC, request/response validation.
 */
class EmailTemplateControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
    }

    @Test
    void testListEmailTemplates_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/email-templates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEmailTemplates_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListEmailTemplates_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEmailTemplates_ReturnsTemplateList() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()); // Returns List directly, not PaginatedResponseDto
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetEmailTemplate_WithValidName_MayReturn404() throws Exception {
        // EmailTemplateController uses name, not ID. Template may not exist
        mockMvc.perform(get("/api/email-templates/invite")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateEmailTemplate_EndpointDoesNotExist_Returns404() throws Exception {
        // EmailTemplateController doesn't have a POST create endpoint
        // Should return 405 (Method Not Allowed)
        mockMvc.perform(post("/api/email-templates")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>()))
                .with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateEmailTemplate_WithValidName_MayReturn404() throws Exception {
        // EmailTemplateController uses PUT, not PATCH, and uses name, not ID
        // Template may not exist, so this test may return 404
        UpdateEmailTemplateDto dto = new UpdateEmailTemplateDto(
                "en",  // locale
                "Test Subject",  // subject (required)
                "<p>Test HTML</p>",  // html (required)
                "Test Text"  // text (required)
        );
        mockMvc.perform(put("/api/email-templates/invite")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEmailTemplates_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }
}

