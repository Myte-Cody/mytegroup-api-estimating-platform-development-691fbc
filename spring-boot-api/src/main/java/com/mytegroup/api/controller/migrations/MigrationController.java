package com.mytegroup.api.controller.migrations;

import com.mytegroup.api.dto.migrations.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/migration")
@PreAuthorize("isAuthenticated()")
public class MigrationController {

    @PostMapping("/start")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> start(@RequestBody @Valid StartMigrationDto dto) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{orgId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> status(@PathVariable String orgId) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/abort")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> abort(@RequestBody @Valid AbortMigrationDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/finalize")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> finalize(@RequestBody @Valid FinalizeMigrationDto dto) {
        return ResponseEntity.ok().build();
    }
}

