package com.mytegroup.api.controller.events;

import com.mytegroup.api.dto.events.ListEventsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@PreAuthorize("isAuthenticated()")
public class EventController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@ModelAttribute ListEventsDto query) {
        return ResponseEntity.ok().build();
    }
}

