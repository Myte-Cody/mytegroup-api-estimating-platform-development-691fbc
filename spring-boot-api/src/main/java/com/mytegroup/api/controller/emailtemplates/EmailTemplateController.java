package com.mytegroup.api.controller.emailtemplates;

import com.mytegroup.api.dto.emailtemplates.*;
import com.mytegroup.api.entity.core.EmailTemplate;
import com.mytegroup.api.service.common.ActorContext;
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
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<EmailTemplate> templates = emailTemplatesService.list(actor, resolvedOrgId);
        
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
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        EmailTemplate template = emailTemplatesService.getByName(name, actor, resolvedOrgId);
        
        return ResponseEntity.ok(templateToMap(template));
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable String name,
            @RequestBody @Valid UpdateEmailTemplateDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        EmailTemplate templateUpdates = new EmailTemplate();
        templateUpdates.setSubject(dto.getSubject());
        templateUpdates.setHtmlBody(dto.getHtmlBody());
        templateUpdates.setTextBody(dto.getTextBody());
        
        EmailTemplate updatedTemplate = emailTemplatesService.update(name, templateUpdates, actor, resolvedOrgId);
        
        return ResponseEntity.ok(templateToMap(updatedTemplate));
    }

    @PostMapping("/{name}/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(
            @PathVariable String name,
            @RequestBody @Valid PreviewEmailTemplateDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, String> preview = emailTemplatesService.preview(name, dto.getVariables(), actor, resolvedOrgId);
        
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/{name}/test-send")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> testSend(
            @PathVariable String name,
            @RequestBody @Valid TestSendTemplateDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        emailTemplatesService.testSend(name, dto.getTo(), dto.getVariables(), actor, resolvedOrgId);
        
        return ResponseEntity.ok(Map.of("status", "ok", "to", dto.getTo()));
    }

    @PostMapping("/{name}/reset")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> reset(
            @PathVariable String name,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        EmailTemplate resetTemplate = emailTemplatesService.resetToDefault(name, actor, resolvedOrgId);
        
        return ResponseEntity.ok(templateToMap(resetTemplate));
    }
    
    // Helper methods
    
    private Map<String, Object> templateToMap(EmailTemplate template) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", template.getId());
        map.put("name", template.getName());
        map.put("subject", template.getSubject());
        map.put("htmlBody", template.getHtmlBody());
        map.put("textBody", template.getTextBody());
        map.put("isDefault", template.getIsDefault());
        map.put("orgId", template.getOrganization() != null ? template.getOrganization().getId() : null);
        map.put("createdAt", template.getCreatedAt());
        map.put("updatedAt", template.getUpdatedAt());
        return map;
    }
    
    private ActorContext getActorContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new ActorContext(null, null, null, null);
        }
        
        Long userId = null;
        if (auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        } else if (auth.getPrincipal() instanceof String) {
            try {
                userId = Long.parseLong((String) auth.getPrincipal());
            } catch (NumberFormatException ignored) {}
        }
        
        return new ActorContext(
            userId != null ? userId.toString() : null,
            null,
            null,
            null
        );
    }
}
