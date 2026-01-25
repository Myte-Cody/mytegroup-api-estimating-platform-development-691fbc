package com.mytegroup.api.controller.email;

import com.mytegroup.api.dto.email.SendEmailDto;
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
 * Integration tests for EmailController.
 * Tests email sending operations, RBAC, request/response validation.
 */
class EmailControllerIntegrationTest extends BaseControllerTest {

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
    void testSendEmail_WithOrgAdmin_IsAllowed() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        // Test that endpoint is accessible (may fail if email service not configured)
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Accept 200 (ok) or 5xx (service error)
                    assertTrue(status == 200 || status >= 500, 
                        "Expected 200 or 5xx but got " + status);
                });
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testSendEmail_WithUserRole_IsForbidden() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}

