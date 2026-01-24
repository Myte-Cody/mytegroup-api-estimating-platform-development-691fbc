package com.mytegroup.api.controller.notifications;

import com.mytegroup.api.dto.response.NotificationResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.communication.Notification;
import com.mytegroup.api.mapper.response.NotificationResponseMapper;
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
    private final NotificationResponseMapper notificationResponseMapper;

    @GetMapping
    public ResponseEntity<PaginatedResponseDto<NotificationResponseDto>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(PaginatedResponseDto.<NotificationResponseDto>builder()
                    .data(java.util.List.of())
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .build());
        }
        Boolean read = unreadOnly != null && unreadOnly ? false : null;
        Page<Notification> notifications = notificationsService.list(orgId, read, page, limit);
        
        return ResponseEntity.ok(PaginatedResponseDto.<NotificationResponseDto>builder()
                .data(notifications.getContent().stream().map(notificationResponseMapper::toDto).toList())
                .total(notifications.getTotalElements())
                .page(page)
                .limit(limit)
                .build());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDto> markRead(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Notification notification = notificationsService.markRead(id, orgId);
        
        return ResponseEntity.ok(notificationResponseMapper.toDto(notification));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllRead(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        // TODO: Implement markAllRead method in service
        throw new UnsupportedOperationException("markAllRead not yet implemented");
    }
    
}
