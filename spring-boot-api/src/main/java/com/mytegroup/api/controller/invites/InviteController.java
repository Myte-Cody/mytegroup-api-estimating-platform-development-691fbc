package com.mytegroup.api.controller.invites;

import com.mytegroup.api.dto.invites.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invites")
@PreAuthorize("isAuthenticated()")
public class InviteController {

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(@RequestBody @Valid CreateInviteDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<?> accept(@PathVariable String token, @RequestBody @Valid AcceptInviteDto dto) {
        return ResponseEntity.ok().build();
    }
}

