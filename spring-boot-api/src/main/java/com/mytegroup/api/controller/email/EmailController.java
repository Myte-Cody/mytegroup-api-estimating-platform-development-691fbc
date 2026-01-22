package com.mytegroup.api.controller.email;

import com.mytegroup.api.dto.email.SendEmailDto;
import com.mytegroup.api.service.email.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> send(
            @RequestBody @Valid SendEmailDto dto,
            @RequestParam(required = false) String orgId) {
        
        emailService.sendEmail(dto.getTo(), dto.getSubject(), dto.getText());
        
        return ResponseEntity.ok(Map.of("status", "ok", "to", dto.getTo()));
    }
    
        }
        
