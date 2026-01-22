package com.mytegroup.api.controller.notifications;

import com.mytegroup.api.entity.communication.Notification;
import com.mytegroup.api.service.notifications.NotificationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationsService notificationsService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Boolean read = unreadOnly != null && unreadOnly ? false : null;
        Page<Notification> notifications = notificationsService.list(orgId, read, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", notifications.getContent().stream().map(this::notificationToMap).toList());
        response.put("total", notifications.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Notification notification = notificationsService.markRead(id, orgId);
        
        return ResponseEntity.ok(notificationToMap(notification));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllRead(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement markAllRead method in service
        throw new UnsupportedOperationException("markAllRead not yet implemented");
    }
    
    // Helper methods
    
    private Map<String, Object> notificationToMap(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("userId", notification.getUser() != null ? notification.getUser().getId() : null);
        map.put("type", notification.getType());
        map.put("payload", notification.getPayload());
        map.put("readAt", notification.getReadAt());
        map.put("orgId", notification.getOrganization() != null ? notification.getOrganization().getId() : null);
        map.put("createdAt", notification.getCreatedAt());
        return map;
    }
    
}
