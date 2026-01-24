package com.mytegroup.api.controller.emailtemplates;

import com.mytegroup.api.dto.emailtemplates.*;
import com.mytegroup.api.dto.response.EmailTemplateResponseDto;
import com.mytegroup.api.entity.communication.EmailTemplate;
import com.mytegroup.api.mapper.response.EmailTemplateResponseMapper;
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
    private final EmailTemplateResponseMapper emailTemplateResponseMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<List<EmailTemplateResponseDto>> list(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        List<EmailTemplate> templates = emailTemplatesService.list(orgId, null);
        
        List<EmailTemplateResponseDto> response = templates.stream()
            .map(emailTemplateResponseMapper::toDto)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<EmailTemplateResponseDto> get(
            @PathVariable String name,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        EmailTemplate template = emailTemplatesService.getTemplate(orgId, name, null);
        
        return ResponseEntity.ok(emailTemplateResponseMapper.toDto(template));
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<EmailTemplateResponseDto> update(
            @PathVariable String name,
            @RequestBody @Valid UpdateEmailTemplateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        // Get template first to get its ID
        EmailTemplate existingTemplate = emailTemplatesService.getTemplate(orgId, name, dto.locale());
        EmailTemplate templateUpdates = new EmailTemplate();
        templateUpdates.setSubject(dto.subject());
        templateUpdates.setHtml(dto.html());
        templateUpdates.setText(dto.text());
        
        EmailTemplate updatedTemplate = emailTemplatesService.update(existingTemplate.getId(), templateUpdates, orgId);
        
        return ResponseEntity.ok(emailTemplateResponseMapper.toDto(updatedTemplate));
    }

    @PostMapping("/{name}/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(
            @PathVariable String name,
            @RequestBody @Valid PreviewEmailTemplateDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
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
            throw new IllegalArgumentException("orgId is required");
        }
        // TODO: Implement testSend method in EmailTemplatesService
        // For now, just return success
        return ResponseEntity.ok(Map.of("status", "ok", "to", dto.to()));
    }

    @PostMapping("/{name}/reset")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<EmailTemplateResponseDto> reset(
            @PathVariable String name,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        // TODO: Implement resetToDefault method in EmailTemplatesService
        // For now, return the existing template
        EmailTemplate template = emailTemplatesService.getTemplate(orgId, name, null);
        return ResponseEntity.ok(emailTemplateResponseMapper.toDto(template));
    }
    
}
