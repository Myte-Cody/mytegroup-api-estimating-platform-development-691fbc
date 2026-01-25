package com.mytegroup.api.service.emailtemplates;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.communication.EmailTemplate;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.communication.EmailTemplateRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for email template management.
 * Handles CRUD operations for email templates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplatesService {
    
    private final EmailTemplateRepository emailTemplateRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    
    /**
     * Gets an email template by name and locale
     */
    @Transactional(readOnly = true)
    public EmailTemplate getTemplate(String orgId, String name, String locale) {
        Long orgIdLong = Long.parseLong(orgId);
        return emailTemplateRepository.findByOrganization_IdAndNameAndLocale(orgIdLong, name, locale)
            .orElseThrow(() -> new ResourceNotFoundException("Email template not found"));
    }
    
    /**
     * Lists email templates for an organization
     */
    @Transactional(readOnly = true)
    public List<EmailTemplate> list(String orgId, String name) {
        Long orgIdLong = Long.parseLong(orgId);
        if (name != null && !name.trim().isEmpty()) {
            return emailTemplateRepository.findByOrganization_IdAndName(orgIdLong, name);
        }
        return emailTemplateRepository.findByOrganization_Id(orgIdLong);
    }
    
    /**
     * Updates an email template
     */
    @Transactional
    public EmailTemplate update(Long id, EmailTemplate templateUpdates, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        EmailTemplate template = emailTemplateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Email template not found"));
        
        if (template.getOrganization() == null || 
            !template.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Email template not found");
        }
        
        // Update fields
        if (templateUpdates.getSubject() != null) {
            template.setSubject(templateUpdates.getSubject());
        }
        if (templateUpdates.getHtml() != null) {
            template.setHtml(templateUpdates.getHtml());
        }
        if (templateUpdates.getText() != null) {
            template.setText(templateUpdates.getText());
        }
        
        EmailTemplate savedTemplate = emailTemplateRepository.save(template);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", savedTemplate.getName());
        metadata.put("locale", savedTemplate.getLocale());
        
        auditLogService.log(
            "email_template.updated",
            orgId,
            null,
            "EmailTemplate",
            savedTemplate.getId().toString(),
            metadata
        );
        
        return savedTemplate;
    }
    
    /**
     * Renders an email template with variables
     */
    @Transactional(readOnly = true)
    public Map<String, String> render(String orgId, String name, String locale, Map<String, Object> variables) {
        EmailTemplate template = getTemplate(orgId, name, locale);
        
        // TODO: Implement template rendering with variable substitution
        Map<String, String> rendered = new HashMap<>();
        rendered.put("subject", template.getSubject());
        rendered.put("html", template.getHtml());
        rendered.put("text", template.getText());
        
        return rendered;
    }
}

