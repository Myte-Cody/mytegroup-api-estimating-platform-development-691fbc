package com.mytegroup.api.service.emailtemplates;

import com.mytegroup.api.entity.communication.EmailTemplate;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.communication.EmailTemplateRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTemplatesServiceTest {

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private EmailTemplatesService emailTemplatesService;

    private Organization testOrganization;
    private EmailTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testTemplate = new EmailTemplate();
        testTemplate.setId(1L);
        testTemplate.setName("invite");
        testTemplate.setLocale("en");
        testTemplate.setSubject("Test Subject");
        testTemplate.setHtml("<html>Test</html>");
        testTemplate.setOrganization(testOrganization);
    }

    @Test
    void testGetTemplate_WithValidNameAndLocale_ReturnsTemplate() {
        String orgId = "1";
        String name = "invite";
        String locale = "en";

        when(emailTemplateRepository.findByOrganization_IdAndNameAndLocale(1L, name, locale))
            .thenReturn(Optional.of(testTemplate));

        EmailTemplate result = emailTemplatesService.getTemplate(orgId, name, locale);

        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(locale, result.getLocale());
    }

    @Test
    void testGetTemplate_WithNonExistentTemplate_ThrowsResourceNotFoundException() {
        String orgId = "1";
        String name = "nonexistent";
        String locale = "en";

        when(emailTemplateRepository.findByOrganization_IdAndNameAndLocale(1L, name, locale))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            emailTemplatesService.getTemplate(orgId, name, locale);
        });
    }

    @Test
    void testList_WithName_ReturnsFilteredList() {
        String orgId = "1";
        String name = "invite";

        when(emailTemplateRepository.findByOrganization_IdAndName(1L, name))
            .thenReturn(List.of(testTemplate));

        List<EmailTemplate> result = emailTemplatesService.list(orgId, name);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(emailTemplateRepository, times(1)).findByOrganization_IdAndName(1L, name);
    }

    @Test
    void testList_WithoutName_ReturnsAllTemplates() {
        String orgId = "1";

        when(emailTemplateRepository.findByOrganization_Id(1L))
            .thenReturn(List.of(testTemplate));

        List<EmailTemplate> result = emailTemplatesService.list(orgId, null);

        assertNotNull(result);
        verify(emailTemplateRepository, times(1)).findByOrganization_Id(1L);
    }

    @Test
    void testUpdate_WithValidUpdates_UpdatesTemplate() {
        Long templateId = 1L;
        String orgId = "1";
        EmailTemplate updates = new EmailTemplate();
        updates.setSubject("Updated Subject");
        updates.setHtml("<html>Updated</html>");

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(emailTemplateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(emailTemplateRepository.save(any(EmailTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailTemplate result = emailTemplatesService.update(templateId, updates, orgId);

        assertNotNull(result);
        assertEquals("Updated Subject", result.getSubject());
        verify(emailTemplateRepository, times(1)).save(any(EmailTemplate.class));
    }

    @Test
    void testUpdate_WithNullOrgId_ThrowsBadRequestException() {
        Long templateId = 1L;
        EmailTemplate updates = new EmailTemplate();

        assertThrows(BadRequestException.class, () -> {
            emailTemplatesService.update(templateId, updates, null);
        });
    }

    @Test
    void testUpdate_WithNonExistentTemplate_ThrowsResourceNotFoundException() {
        Long templateId = 999L;
        String orgId = "1";
        EmailTemplate updates = new EmailTemplate();

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(emailTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            emailTemplatesService.update(templateId, updates, orgId);
        });
    }
}


