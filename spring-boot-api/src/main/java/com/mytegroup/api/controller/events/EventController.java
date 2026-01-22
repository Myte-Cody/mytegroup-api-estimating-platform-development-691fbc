package com.mytegroup.api.controller.events;

import com.mytegroup.api.dto.events.ListEventsDto;
import com.mytegroup.api.service.common.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class EventController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @ModelAttribute ListEventsDto query,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement listEvents method in AuditLogService
        throw new UnsupportedOperationException("listEvents not yet implemented");
    }
    
}
