package com.mytegroup.api.controller.companies;

import com.mytegroup.api.dto.companies.CompaniesImportConfirmDto;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.companiesimport.CompaniesImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/companies/import/v1")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CompaniesImportController {

    private final CompaniesImportService companiesImportService;

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> preview = companiesImportService.preview(file, actor, resolvedOrgId);
        
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> confirm(
            @RequestBody @Valid CompaniesImportConfirmDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> result = companiesImportService.confirm(dto.getPreviewId(), dto.getConfirmedRows(), actor, resolvedOrgId);
        
        return ResponseEntity.ok(result);
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
