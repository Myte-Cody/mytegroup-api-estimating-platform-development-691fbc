package com.mytegroup.api.controller.migrations;

import com.mytegroup.api.dto.migrations.*;
import com.mytegroup.api.service.migrations.MigrationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/migration")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationsService migrationsService;

    @PostMapping("/start")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> start(@RequestBody @Valid StartMigrationDto dto) {
                Map<String, Object> result = migrationsService.start(dto.getOrgId(), dto.getTargetDatastoreType());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status/{orgId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> status(@PathVariable String orgId) {
                Map<String, Object> status = migrationsService.getStatus(orgId);
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/abort")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> abort(@RequestBody @Valid AbortMigrationDto dto) {
                Map<String, Object> result = migrationsService.abort(dto.getOrgId());
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/finalize")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> finalize(@RequestBody @Valid FinalizeMigrationDto dto) {
                Map<String, Object> result = migrationsService.finalize(dto.getOrgId());
        
        return ResponseEntity.ok(result);
    }
}
