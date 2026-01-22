package com.mytegroup.api.controller.emailtemplates;

import com.mytegroup.api.dto.emailtemplates.*;
import com.mytegroup.api.entity.communication.EmailTemplate;
import com.mytegroup.api.service.emailtemplates.EmailTemplatesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email-templates")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplatesService emailTemplatesService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        List<EmailTemplate> templates = emailTemplatesService.list(orgId, null);
        
        List<Map<String, Object>> response = templates.stream()
            .map(this::templateToMap)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> get(
            @PathVariable String name,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        EmailTemplate template = emailTemplatesService.getTemplate(orgId, name, null);
        
        return ResponseEntity.ok(templateToMap(template));
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable String name,
            @RequestBody @Valid UpdateEmailTemplateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // Get template first to get its ID
        EmailTemplate existingTemplate = emailTemplatesService.getTemplate(orgId, name, dto.locale());
        EmailTemplate templateUpdates = new EmailTemplate();
        templateUpdates.setSubject(dto.subject());
        templateUpdates.setHtml(dto.html());
        templateUpdates.setText(dto.text());
        
        EmailTemplate updatedTemplate = emailTemplatesService.update(existingTemplate.getId(), templateUpdates, orgId);
        
        return ResponseEntity.ok(templateToMap(updatedTemplate));
    }

    @PostMapping("/{name}/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(
            @PathVariable String name,
            @RequestBody @Valid PreviewEmailTemplateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Map<String, String> preview = emailTemplatesService.render(orgId, name, dto.locale(), dto.variables());
        
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/{name}/test-send")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> testSend(
            @PathVariable String name,
            @RequestBody @Valid TestSendTemplateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement testSend method in EmailTemplatesService
        // For now, just return success
        return ResponseEntity.ok(Map.of("status", "ok", "to", dto.to()));
    }

    @PostMapping("/{name}/reset")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> reset(
            @PathVariable String name,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement resetToDefault method in EmailTemplatesService
        // For now, return the existing template
        EmailTemplate template = emailTemplatesService.getTemplate(orgId, name, null);
        return ResponseEntity.ok(templateToMap(template));
    }
    
    // Helper methods
    
    private Map<String, Object> templateToMap(EmailTemplate template) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", template.getId());
        map.put("name", template.getName());
        map.put("locale", template.getLocale());
        map.put("subject", template.getSubject());
        map.put("html", template.getHtml());
        map.put("text", template.getText());
        map.put("orgId", template.getOrganization() != null ? template.getOrganization().getId() : null);
        map.put("createdAt", template.getCreatedAt());
        map.put("updatedAt", template.getUpdatedAt());
        return map;
    }
}
