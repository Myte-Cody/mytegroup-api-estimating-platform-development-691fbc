package com.mytegroup.api.controller.bulk;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bulk-import")
@PreAuthorize("isAuthenticated()")
public class BulkController {

    @PostMapping("/{entityType}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> importEntities(
        @PathVariable String entityType,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{entityType}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> exportEntities(@PathVariable String entityType) {
        return ResponseEntity.ok().build();
    }
}
