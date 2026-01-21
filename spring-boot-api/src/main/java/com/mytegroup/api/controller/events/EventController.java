package com.mytegroup.api.controller.events;

import com.mytegroup.api.dto.events.ListEventsDto;
import com.mytegroup.api.service.common.ActorContext;
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
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<Map<String, Object>> events = auditLogService.listEvents(
            query.getEntityType(),
            query.getEntityId(),
            query.getAction(),
            query.getActorId(),
            query.getFrom(),
            query.getTo(),
            query.getPage(),
            query.getLimit(),
            actor,
            resolvedOrgId
        );
        
        return ResponseEntity.ok(events);
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
