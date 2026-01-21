package com.mytegroup.api.controller.compliance;

import com.mytegroup.api.dto.compliance.*;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.compliance.ComplianceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    @PostMapping("/strip-pii")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> stripPii(@RequestBody @Valid StripPiiDto dto) {
        ActorContext actor = getActorContext();
        
        Map<String, Object> result = complianceService.stripPii(
            dto.getEntityType(),
            dto.getEntityId(),
            dto.getOrgId(),
            actor
        );
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/legal-hold")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> setLegalHold(@RequestBody @Valid SetLegalHoldDto dto) {
        ActorContext actor = getActorContext();
        
        Map<String, Object> result = complianceService.setLegalHold(
            dto.getEntityType(),
            dto.getEntityId(),
            dto.getLegalHold(),
            dto.getOrgId(),
            actor
        );
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch-archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> batchArchive(@RequestBody @Valid BatchArchiveDto dto) {
        ActorContext actor = getActorContext();
        
        Map<String, Object> result = complianceService.batchArchive(
            dto.getEntityType(),
            dto.getEntityIds(),
            dto.getOrgId(),
            actor
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
