package com.mytegroup.api.controller.sessions;

import com.mytegroup.api.dto.sessions.RevokeSessionDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@PreAuthorize("isAuthenticated()")
public class SessionController {

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestBody @Valid RevokeSessionDto dto) {
        return ResponseEntity.ok().build();
    }
}

