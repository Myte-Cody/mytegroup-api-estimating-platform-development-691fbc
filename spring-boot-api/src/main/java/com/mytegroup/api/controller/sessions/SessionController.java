package com.mytegroup.api.controller.sessions;

import com.mytegroup.api.dto.sessions.RevokeSessionDto;
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
import java.util.Set;

@RestController
@RequestMapping("/api/sessions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SessionController {

    private final SessionsService sessionsService;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Get userId from session when sessions are implemented
        Set<Object> sessionIds = sessionsService.listUserSessions(null);
        
        List<Map<String, Object>> response = sessionIds.stream()
            .map(sessionId -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", sessionId);
                return map;
            })
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(
            @RequestBody @Valid RevokeSessionDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Get userId from session when sessions are implemented
        sessionsService.removeSession(dto.sessionId(), null);
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    @PostMapping("/revoke-all")
    public ResponseEntity<?> revokeAll(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Get userId from session when sessions are implemented
        sessionsService.removeAllUserSessions(null);
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
