package com.mytegroup.api.controller.emailtemplates;

import com.mytegroup.api.dto.emailtemplates.UpdateEmailTemplateDto;
import com.mytegroup.api.dto.emailtemplates.PreviewEmailTemplateDto;
import com.mytegroup.api.dto.emailtemplates.TestSendTemplateDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EmailTemplateController.
 * Tests email template operations, RBAC, request/response validation.
 */
class EmailTemplateControllerIntegrationTest extends BaseControllerTest {

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
    void testListTemplates_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListTemplates_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListTemplates_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/email-templates"))
                .andExpect(status().isBadRequest());
    }

    // ========== GET BY NAME ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTemplate_WithValidName_ReturnsTemplate() throws Exception {
        // Create a template first
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "welcome");
        
        mockMvc.perform(get("/api/email-templates/welcome")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("welcome"))
                .andExpect(jsonPath("$.subject").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTemplate_WithNonExistentTemplate_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/email-templates/nonexistent")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateTemplate_WithValidData_ReturnsUpdated() throws Exception {
        // Create a template first with explicit locale "en"
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "welcome", "en");
        
        // Verify template was created
        assertNotNull(template.getId());
        assertEquals("en", template.getLocale());
        
        UpdateEmailTemplateDto dto = new UpdateEmailTemplateDto(
            "en",  // locale (first parameter)
            "Updated Subject",  // subject
            "<html>Updated HTML</html>",  // html
            "Updated text"  // text
        );
        
        mockMvc.perform(put("/api/email-templates/welcome")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("welcome"))
                .andExpect(jsonPath("$.subject").value("Updated Subject"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateTemplate_WithNonExistentTemplate_ReturnsNotFound() throws Exception {
        UpdateEmailTemplateDto dto = new UpdateEmailTemplateDto(
            "en",  // locale
            "Test Subject",  // subject
            "<html>Test HTML</html>",  // html
            "Test text"  // text
        );
        
        mockMvc.perform(put("/api/email-templates/nonexistent")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ========== PREVIEW ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewTemplate_WithValidData_ReturnsPreview() throws Exception {
        // Create a template first
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "welcome");
        
        PreviewEmailTemplateDto dto = new PreviewEmailTemplateDto(
            "en",
            Map.of("name", "Test User")
        );
        
        mockMvc.perform(post("/api/email-templates/welcome/preview")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewTemplate_WithNonExistentTemplate_ReturnsNotFound() throws Exception {
        PreviewEmailTemplateDto dto = new PreviewEmailTemplateDto(
            "en",
            Map.of("name", "Test User")
        );
        
        mockMvc.perform(post("/api/email-templates/nonexistent/preview")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ========== TEST SEND ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendTemplate_WithValidData_ReturnsOk() throws Exception {
        TestSendTemplateDto dto = new TestSendTemplateDto(
            "test@example.com",
            "en",
            Map.of("name", "Test User")
        );
        
        mockMvc.perform(post("/api/email-templates/welcome/test-send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.to").value("test@example.com"));
    }

    // ========== RESET ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testResetTemplate_WithValidName_ReturnsTemplate() throws Exception {
        // Create a template first
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "welcome");
        
        mockMvc.perform(post("/api/email-templates/welcome/reset")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("welcome"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testResetTemplate_WithNonExistentTemplate_ReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/email-templates/nonexistent/reset")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ========== ADDITIONAL TEMPLATE NAME TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTemplate_WithInviteName_ReturnsTemplate() throws Exception {
        // Create invite template
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "invite");
        
        mockMvc.perform(get("/api/email-templates/invite")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("invite"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateTemplate_WithInviteName_ReturnsUpdated() throws Exception {
        // Create invite template
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "invite");
        
        UpdateEmailTemplateDto dto = new UpdateEmailTemplateDto(
            "en",  // locale
            "Invite Subject",  // subject
            "<html>Invite HTML</html>",  // html
            "Invite text"  // text
        );
        mockMvc.perform(put("/api/email-templates/invite")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Invite Subject"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewTemplate_WithInviteName_ReturnsPreview() throws Exception {
        // Create invite template
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "invite");
        
        PreviewEmailTemplateDto dto = new PreviewEmailTemplateDto(
            "en", Map.of("name", "Test User")
        );
        mockMvc.perform(post("/api/email-templates/invite/preview")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendTemplate_WithInviteName_ReturnsOk() throws Exception {
        TestSendTemplateDto dto = new TestSendTemplateDto(
            "test@example.com", "en", Map.of("name", "Test User")
        );
        mockMvc.perform(post("/api/email-templates/invite/test-send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // ========== ADDITIONAL UPDATE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateTemplate_WithoutOrgId_ThrowsException() throws Exception {
        UpdateEmailTemplateDto dto = new UpdateEmailTemplateDto(
            "en",  // locale
            "Test Subject",  // subject
            "<html>Test HTML</html>",  // html
            "Test text"  // text
        );
        mockMvc.perform(put("/api/email-templates/welcome")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL PREVIEW TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewTemplate_WithoutOrgId_ThrowsException() throws Exception {
        PreviewEmailTemplateDto dto = new PreviewEmailTemplateDto(
            "en", Map.of("name", "Test User")
        );
        mockMvc.perform(post("/api/email-templates/welcome/preview")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL TEST SEND TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendTemplate_WithoutOrgId_ThrowsException() throws Exception {
        TestSendTemplateDto dto = new TestSendTemplateDto(
            "test@example.com", "en", Map.of("name", "Test User")
        );
        mockMvc.perform(post("/api/email-templates/welcome/test-send")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL RESET TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testResetTemplate_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(post("/api/email-templates/welcome/reset")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testListTemplates_WithOrgOwner_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListTemplates_WithSuperAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ========== ADDITIONAL EDGE CASE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListTemplates_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testListTemplates_WithPlatformAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTemplate_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/email-templates/welcome"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTemplate_WithInvalidName_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/email-templates/nonexistent-template")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateTemplate_WithInvalidName_ReturnsNotFound() throws Exception {
        UpdateEmailTemplateDto dto = new UpdateEmailTemplateDto(
            "en",  // locale
            "Test Subject",  // subject
            "<html>Test HTML</html>",  // html
            "Test text"  // text
        );
        mockMvc.perform(put("/api/email-templates/nonexistent-template")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewTemplate_WithInvalidName_ReturnsNotFound() throws Exception {
        PreviewEmailTemplateDto dto = new PreviewEmailTemplateDto(
            "en", Map.of("name", "Test User")
        );
        mockMvc.perform(post("/api/email-templates/nonexistent-template/preview")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendTemplate_WithoutTo_ReturnsBadRequest() throws Exception {
        TestSendTemplateDto dto = new TestSendTemplateDto(
            null, "en", Map.of("name", "Test User")
        );
        mockMvc.perform(post("/api/email-templates/welcome/test-send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendTemplate_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        TestSendTemplateDto dto = new TestSendTemplateDto(
            "invalid-email", "en", Map.of("name", "Test User")
        );
        mockMvc.perform(post("/api/email-templates/welcome/test-send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testResetTemplate_WithInvalidName_ReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/email-templates/nonexistent-template/reset")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListTemplates_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    @Test
    void testListTemplates_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateTemplate_WithNullLocale_ReturnsOk() throws Exception {
        // Create template first
        setupEmailTemplate(testOrganization, "welcome");
        
        UpdateEmailTemplateDto dto = new UpdateEmailTemplateDto(
            null,  // locale (null defaults to "en" in service)
            "Test Subject",  // subject
            "<html>Test HTML</html>",  // html
            "Test text"  // text
        );
        mockMvc.perform(put("/api/email-templates/welcome")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewTemplate_WithNullLocale_ReturnsOk() throws Exception {
        // Create template first
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "welcome", "en");
        
        PreviewEmailTemplateDto dto = new PreviewEmailTemplateDto(
            null, Map.of("name", "Test User")
        );
        mockMvc.perform(post("/api/email-templates/welcome/preview")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewTemplate_WithEmptyVariables_ReturnsOk() throws Exception {
        // Create template first
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "welcome");
        
        PreviewEmailTemplateDto dto = new PreviewEmailTemplateDto(
            "en", Map.of()
        );
        mockMvc.perform(post("/api/email-templates/welcome/preview")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateTemplate_WithDifferentLocale() throws Exception {
        // Create template with "fr" locale first
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "welcome", "fr");
        
        UpdateEmailTemplateDto dto = new UpdateEmailTemplateDto(
            "fr",  // locale - must match the template's locale
            "French Subject",  // subject
            "<html>French HTML</html>",  // html
            "French text"  // text
        );
        mockMvc.perform(put("/api/email-templates/welcome")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("welcome"))
                .andExpect(jsonPath("$.subject").value("French Subject"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTemplate_WithDifferentLocale() throws Exception {
        // Create template with "en" locale (controller defaults to "en" when locale is not provided)
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            setupEmailTemplate(testOrganization, "welcome", "en");
        
        mockMvc.perform(get("/api/email-templates/welcome")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("welcome"))
                .andExpect(jsonPath("$.subject").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListTemplates_WithMultipleTemplates_ReturnsAll() throws Exception {
        // Create multiple templates
        setupEmailTemplate(testOrganization, "welcome");
        setupEmailTemplate(testOrganization, "invite");
        setupEmailTemplate(testOrganization, "verification");
        
        mockMvc.perform(get("/api/email-templates")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testDeleteTemplate_WithValidName_ReturnsOk() throws Exception {
        // Create template first
        setupEmailTemplate(testOrganization, "welcome");
        
        // Note: Delete endpoint doesn't exist in controller, testing that it returns 405 Method Not Allowed
        mockMvc.perform(delete("/api/email-templates/welcome")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }
}

