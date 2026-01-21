package com.mytegroup.api.controller.sessions;

import com.mytegroup.api.dto.sessions.RevokeSessionDto;
import com.mytegroup.api.entity.core.Session;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.sessions.SessionsService;
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
@RequestMapping("/api/sessions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SessionController {

    private final SessionsService sessionsService;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String orgId) {
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<Session> sessions = sessionsService.listForUser(actor, resolvedOrgId);
        
        List<Map<String, Object>> response = sessions.stream()
            .map(this::sessionToMap)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(
            @RequestBody @Valid RevokeSessionDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        sessionsService.revoke(Long.parseLong(dto.getSessionId()), actor, resolvedOrgId);
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    @PostMapping("/revoke-all")
    public ResponseEntity<?> revokeAll(@RequestParam(required = false) String orgId) {
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        int revokedCount = sessionsService.revokeAllForUser(actor, resolvedOrgId);
        
        return ResponseEntity.ok(Map.of("status", "ok", "revoked", revokedCount));
    }
    
    // Helper methods
    
    private Map<String, Object> sessionToMap(Session session) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("userId", session.getUser() != null ? session.getUser().getId() : null);
        map.put("isRevoked", session.getIsRevoked());
        map.put("expiresAt", session.getExpiresAt());
        map.put("createdAt", session.getCreatedAt());
        map.put("updatedAt", session.getUpdatedAt());
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
