package com.mytegroup.api.controller.notifications;

import com.mytegroup.api.dto.notifications.ListNotificationsQueryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    @GetMapping
    public ResponseEntity<?> list(@ModelAttribute ListNotificationsQueryDto query) {
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }
}

