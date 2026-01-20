package com.mytegroup.api.controller.seats;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seats")
@PreAuthorize("isAuthenticated()")
public class SeatController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@RequestParam(required = false) String orgId) {
        return ResponseEntity.ok().build();
    }
}
