package com.mytegroup.api.controller.people;

import com.mytegroup.api.dto.people.*;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.peopleimport.PeopleImportService;
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
@RequestMapping("/api/people/import")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PeopleImportController {

    private final PeopleImportService peopleImportService;

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> preview = peopleImportService.preview(file, actor, resolvedOrgId);
        
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> confirm(
            @RequestBody @Valid PeopleImportConfirmDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> result = peopleImportService.confirm(dto.getPreviewId(), dto.getConfirmedRows(), actor, resolvedOrgId);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/v1/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> previewV1(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> preview = peopleImportService.previewV1(file, actor, resolvedOrgId);
        
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/v1/confirm")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> confirmV1(
            @RequestBody @Valid PeopleImportV1ConfirmDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> result = peopleImportService.confirmV1(dto, actor, resolvedOrgId);
        
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
