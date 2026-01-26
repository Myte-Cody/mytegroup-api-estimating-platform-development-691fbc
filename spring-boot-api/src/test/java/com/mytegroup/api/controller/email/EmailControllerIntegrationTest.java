package com.mytegroup.api.controller.email;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.mytegroup.api.dto.email.SendEmailDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EmailController.
 * Tests email sending operations, RBAC, request/response validation.
 * Uses GreenMail for email testing.
 */
class EmailControllerIntegrationTest extends BaseControllerTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(
        new ServerSetup(1025, null, ServerSetup.PROTOCOL_SMTP)
    )
    .withConfiguration(
        com.icegreen.greenmail.configuration.GreenMailConfiguration.aConfig()
            .withUser("test@localhost", "test")
    );

    private Organization testOrganization;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        // Clear any messages from previous tests
        try {
            greenMail.purgeEmailFromAllMailboxes();
        } catch (Exception e) {
            // Ignore - mailbox may not exist yet
        }
    }

    /**
     * Helper method to verify that GreenMail received an email with the expected content.
     * 
     * @param expectedTo Expected recipient email address
     * @param expectedSubject Expected email subject
     * @param expectedText Expected email text content (can be null)
     * @throws Exception if email verification fails
     */
    private void verifyEmailReceived(String expectedTo, String expectedSubject, String expectedText) throws Exception {
        // Wait a bit for email to be received (GreenMail processes asynchronously)
        assertTrue(greenMail.waitForIncomingEmail(5000, 1), 
            "Expected at least 1 email to be received by GreenMail");
        
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertTrue(receivedMessages.length > 0, 
            "GreenMail should have received at least one email");
        
        MimeMessage message = receivedMessages[receivedMessages.length - 1];
        
        // Verify recipient
        Address[] recipients = message.getRecipients(MimeMessage.RecipientType.TO);
        assertNotNull(recipients, "Email should have recipients");
        assertEquals(1, recipients.length, "Email should have exactly one recipient");
        assertEquals(expectedTo, recipients[0].toString(), 
            "Email recipient should match expected value");
        
        // Verify subject
        assertEquals(expectedSubject, message.getSubject(), 
            "Email subject should match expected value");
        
        // Verify text content if provided
        if (expectedText != null && !expectedText.isEmpty()) {
            String content = (String) message.getContent();
            assertTrue(content.contains(expectedText), 
                "Email content should contain expected text: " + expectedText);
        }
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_WithOrgAdmin_IsAllowed() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.to").value("test@example.com"));
        
        // Verify email was received by GreenMail
        verifyEmailReceived("test@example.com", "Test Subject", "Test email body");
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

    // ========== ADDITIONAL EDGE CASE TESTS ==========

    @Test
    void testSendEmail_WithoutAuthentication_Returns401() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testSendEmail_WithAdmin_IsAllowed() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("admin@example.com");
        dto.setSubject("Admin Test Subject");
        dto.setText("Admin test email body");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
        
        // Verify email was received by GreenMail
        verifyEmailReceived("admin@example.com", "Admin Test Subject", "Admin test email body");
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testSendEmail_WithSuperAdmin_IsAllowed() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("superadmin@example.com");
        dto.setSubject("Super Admin Test");
        dto.setText("Super admin test email");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testSendEmail_WithOrgOwner_IsAllowed() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("owner@example.com");
        dto.setSubject("Owner Test");
        dto.setText("Owner test email");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testSendEmail_WithPlatformAdmin_IsAllowed() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("platform@example.com");
        dto.setSubject("Platform Admin Test");
        dto.setText("Platform admin test email");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_WithoutTo_ReturnsBadRequest() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        // Missing 'to' field
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_WithoutSubject_ReturnsBadRequest() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setText("Test email body");
        // Missing 'subject' field
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_WithoutText_StillWorks() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setSubject("Test Subject");
        // Missing 'text' field - text is optional
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("invalid-email");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_ResponseContainsStatus() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.to").value("test@example.com"));
        
        // Verify email was received by GreenMail
        verifyEmailReceived("test@example.com", "Test Subject", "Test email body");
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_ResponseContainsTo() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("recipient@example.com");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.to").value("recipient@example.com"));
        
        // Verify email was received by GreenMail
        verifyEmailReceived("recipient@example.com", "Test Subject", "Test email body");
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_ReturnsJsonContentType() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_WithoutOrgId_StillWorks() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("test@example.com");
        dto.setSubject("Test Subject");
        dto.setText("Test email body");
        
        // orgId is optional in the controller
        mockMvc.perform(post("/api/email/send")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
        
        // Verify email was received by GreenMail
        verifyEmailReceived("test@example.com", "Test Subject", "Test email body");
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_WithHtmlContent() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("html@example.com");
        dto.setSubject("HTML Email Test");
        dto.setText("Plain text version");
        dto.setHtml("<html><body><h1>HTML Version</h1></body></html>");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
        
        // Verify email was received by GreenMail
        verifyEmailReceived("html@example.com", "HTML Email Test", "Plain text version");
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSendEmail_EmailContentIsCorrect() throws Exception {
        SendEmailDto dto = new SendEmailDto();
        dto.setTo("content@example.com");
        dto.setSubject("Content Test");
        dto.setText("This is the email body content");
        
        mockMvc.perform(post("/api/email/send")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
        
        // Verify email was received by GreenMail with correct content
        verifyEmailReceived("content@example.com", "Content Test", "This is the email body content");
    }
}

