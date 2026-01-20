package com.mytegroup.api.controller.legal;

import com.mytegroup.api.dto.legal.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/legal")
@PreAuthorize("isAuthenticated()")
public class LegalController {

    @PostMapping("/docs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createDoc(@RequestBody @Valid CreateLegalDocDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(@RequestBody @Valid AcceptLegalDocDto dto) {
        return ResponseEntity.ok().build();
    }
}

