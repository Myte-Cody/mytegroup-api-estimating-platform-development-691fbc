package com.mytegroup.api.controller.costcodes;

import com.mytegroup.api.dto.costcodes.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cost-codes")
@PreAuthorize("isAuthenticated()")
public class CostCodeController {

    @GetMapping
    public ResponseEntity<?> list(@ModelAttribute ListCostCodesQueryDto query) {
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(@RequestBody @Valid CreateCostCodeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody @Valid UpdateCostCodeDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> toggle(@PathVariable String id, @RequestBody @Valid ToggleCostCodeDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> bulk(@RequestBody @Valid BulkCostCodesDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/seed")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> seed(@RequestBody @Valid SeedCostCodesDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> importPreview(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import/commit")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> importCommit(@RequestBody @Valid CostCodeImportCommitDto dto) {
        return ResponseEntity.ok().build();
    }
}

