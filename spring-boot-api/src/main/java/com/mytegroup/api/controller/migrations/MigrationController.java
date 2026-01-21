package com.mytegroup.api.controller.migrations;

import com.mytegroup.api.dto.migrations.*;
import com.mytegroup.api.service.common.ActorContext;
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
        ActorContext actor = getActorContext();
        
        Map<String, Object> result = migrationsService.start(dto.getOrgId(), dto.getTargetDatastoreType(), actor);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status/{orgId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> status(@PathVariable String orgId) {
        ActorContext actor = getActorContext();
        
        Map<String, Object> status = migrationsService.getStatus(orgId, actor);
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/abort")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> abort(@RequestBody @Valid AbortMigrationDto dto) {
        ActorContext actor = getActorContext();
        
        Map<String, Object> result = migrationsService.abort(dto.getOrgId(), actor);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/finalize")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> finalize(@RequestBody @Valid FinalizeMigrationDto dto) {
        ActorContext actor = getActorContext();
        
        Map<String, Object> result = migrationsService.finalize(dto.getOrgId(), actor);
        
        return ResponseEntity.ok(result);
    }
    
    private ActorContext getActorContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new ActorContext(null, null, null, null);
        }
        
        Long userId = null;
        if (auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        } else if (auth.getPrincipal() instanceof String) {
            try {
                userId = Long.parseLong((String) auth.getPrincipal());
            } catch (NumberFormatException ignored) {}
        }
        
        return new ActorContext(
            userId != null ? userId.toString() : null,
            null,
            null,
            null
        );
    }
}
