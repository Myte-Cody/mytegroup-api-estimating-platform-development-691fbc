package com.mytegroup.api.controller.compliance;

import com.mytegroup.api.dto.compliance.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compliance")
@PreAuthorize("isAuthenticated()")
public class ComplianceController {

    @PostMapping("/strip-pii")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> stripPii(@RequestBody @Valid StripPiiDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/legal-hold")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> setLegalHold(@RequestBody @Valid SetLegalHoldDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> batchArchive(@RequestBody @Valid BatchArchiveDto dto) {
        return ResponseEntity.ok().build();
    }
}

