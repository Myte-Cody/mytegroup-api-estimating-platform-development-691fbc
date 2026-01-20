package com.mytegroup.api.controller.email;

import com.mytegroup.api.dto.email.SendEmailDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@PreAuthorize("isAuthenticated()")
public class EmailController {

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> send(@RequestBody @Valid SendEmailDto dto) {
        return ResponseEntity.ok().build();
    }
}

