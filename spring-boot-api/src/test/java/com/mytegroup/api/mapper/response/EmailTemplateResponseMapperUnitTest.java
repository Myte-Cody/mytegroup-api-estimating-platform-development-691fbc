package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.EmailTemplateResponseDto;
import com.mytegroup.api.entity.communication.EmailTemplate;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateResponseMapperUnitTest {

    private EmailTemplateResponseMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new EmailTemplateResponseMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");
    }

    @Test
    void testEmailTemplateToDto() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 10, 10, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 15, 30, 0);

        EmailTemplate template = new EmailTemplate();
        template.setId(1L);
        template.setName("Welcome Email");
        template.setSubject("Welcome to our platform!");
        template.setHtml("<html><body>Welcome</body></html>");
        template.setText("Welcome");
        template.setOrganization(organization);
        template.setCreatedAt(createdAt);
        template.setUpdatedAt(updatedAt);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Welcome Email", dto.getName());
        assertEquals("Welcome to our platform!", dto.getSubject());
        assertEquals("<html><body>Welcome</body></html>", dto.getBody());
        assertEquals("1", dto.getOrgId());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    void testEmailTemplateToDtoWithNullHtmlFallbackToText() {
        // Arrange
        EmailTemplate template = new EmailTemplate();
        template.setId(2L);
        template.setName("Notification Email");
        template.setSubject("You have a notification");
        template.setHtml(null);
        template.setText("You have a new notification");
        template.setOrganization(organization);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertNotNull(dto);
        assertEquals("You have a new notification", dto.getBody());
    }

    @Test
    void testEmailTemplateToDtoWithHtmlPreferredOverText() {
        // Arrange
        EmailTemplate template = new EmailTemplate();
        template.setId(3L);
        template.setName("HTML Email");
        template.setSubject("HTML preferred");
        template.setHtml("<p>HTML content</p>");
        template.setText("Text content");
        template.setOrganization(organization);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertEquals("<p>HTML content</p>", dto.getBody());
    }

    @Test
    void testEmailTemplateToDtoWithNullOrganization() {
        // Arrange
        EmailTemplate template = new EmailTemplate();
        template.setId(4L);
        template.setName("No Org Email");
        template.setSubject("Test");
        template.setHtml("<p>Content</p>");
        template.setOrganization(null);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getOrgId());
    }

    @Test
    void testEmailTemplateToDtoNull() {
        // Act
        EmailTemplateResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testEmailTemplateToDtoMapsAllFields() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 8, 0, 0);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 20, 16, 45, 0);

        EmailTemplate template = new EmailTemplate();
        template.setId(99L);
        template.setName("Complete Template");
        template.setSubject("Test Subject Line");
        template.setHtml("<html><body><h1>Test</h1></body></html>");
        template.setText("Plain text version");
        template.setOrganization(organization);
        template.setCreatedAt(created);
        template.setUpdatedAt(updated);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertEquals(99L, dto.getId());
        assertEquals("Complete Template", dto.getName());
        assertEquals("Test Subject Line", dto.getSubject());
        assertEquals("<html><body><h1>Test</h1></body></html>", dto.getBody());
        assertEquals("1", dto.getOrgId());
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
    }

    @Test
    void testEmailTemplateToDtoWithDifferentOrganizations() {
        // Arrange
        Organization org2 = new Organization();
        org2.setId(42L);

        EmailTemplate template = new EmailTemplate();
        template.setId(5L);
        template.setName("Other Org Email");
        template.setSubject("Different Org");
        template.setHtml("<p>Content</p>");
        template.setOrganization(org2);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertEquals("42", dto.getOrgId());
    }

    @Test
    void testEmailTemplateToDtoPreservesNameAndSubject() {
        // Arrange
        EmailTemplate template = new EmailTemplate();
        template.setId(6L);
        template.setName("Important Announcement");
        template.setSubject("Read this important update");
        template.setHtml("<strong>Important</strong>");
        template.setOrganization(organization);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertEquals("Important Announcement", dto.getName());
        assertEquals("Read this important update", dto.getSubject());
    }

    @Test
    void testEmailTemplateToDtoWithEmptyHtmlAndText() {
        // Arrange
        EmailTemplate template = new EmailTemplate();
        template.setId(7L);
        template.setName("Empty Template");
        template.setSubject("Empty");
        template.setHtml("");
        template.setText("");
        template.setOrganization(organization);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertNotNull(dto);
        assertEquals("", dto.getBody()); // Empty string is truthy, so HTML wins
    }

    @Test
    void testEmailTemplateToDtoWithNullHtmlAndNullText() {
        // Arrange
        EmailTemplate template = new EmailTemplate();
        template.setId(8L);
        template.setName("No Content");
        template.setSubject("Missing body");
        template.setHtml(null);
        template.setText(null);
        template.setOrganization(organization);

        // Act
        EmailTemplateResponseDto dto = mapper.toDto(template);

        // Assert
        assertNull(dto.getBody());
    }
}



