package com.mytegroup.api.controller.ingestion;

import com.mytegroup.api.dto.ingestion.*;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.ingestioncontacts.IngestionContactsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ingestion/contacts/v1")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class IngestionContactsController {

    private final IngestionContactsService ingestionContactsService;

    @PostMapping("/suggest-mapping")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> suggestMapping(
            @RequestBody @Valid IngestionContactsSuggestMappingDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> mapping = ingestionContactsService.suggestMapping(
            dto.getHeaders(),
            dto.getSampleRows(),
            actor,
            resolvedOrgId
        );
        
        return ResponseEntity.ok(mapping);
    }

    @PostMapping("/parse-row")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> parseRow(
            @RequestBody @Valid IngestionContactsParseRowDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> result = ingestionContactsService.parseRow(
            dto.getRow(),
            dto.getMapping(),
            actor,
            resolvedOrgId
        );
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/enrich")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> enrich(
            @RequestBody @Valid IngestionContactsEnrichDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> result = ingestionContactsService.enrich(
            dto.getContact(),
            actor,
            resolvedOrgId
        );
        
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
