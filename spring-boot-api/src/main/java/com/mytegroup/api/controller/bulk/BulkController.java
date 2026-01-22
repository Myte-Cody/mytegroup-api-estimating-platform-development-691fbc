package com.mytegroup.api.controller.bulk;

import com.mytegroup.api.service.bulk.BulkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/bulk-import")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class BulkController {

    private final BulkService bulkService;

    @PostMapping("/{entityType}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> importEntities(
            @PathVariable String entityType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Map<String, Object> result = bulkService.importEntities(entityType, file, orgId);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{entityType}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> exportEntities(
            @PathVariable String entityType,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        byte[] exportData = bulkService.exportEntities(entityType, orgId);
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=\"" + entityType + "-export.csv\"")
            .body(exportData);
    }
}
