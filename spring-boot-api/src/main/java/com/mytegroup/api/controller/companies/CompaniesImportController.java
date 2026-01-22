package com.mytegroup.api.controller.companies;

import com.mytegroup.api.dto.companies.CompaniesImportConfirmDto;
import com.mytegroup.api.service.companies.CompaniesImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies/import/v1")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CompaniesImportController {

    private final CompaniesImportService companiesImportService;

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement file parsing and preview
        throw new UnsupportedOperationException("File preview not yet implemented - need to parse file first");
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> confirm(
            @RequestBody @Valid CompaniesImportConfirmDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // Convert DTO rows to Map format expected by service
        List<Map<String, Object>> confirmedRows = dto.getConfirmedRows().stream()
            .map(row -> {
                Map<String, Object> map = new java.util.HashMap<>();
                // TODO: Map DTO fields to map
                return map;
            })
            .toList();
        Map<String, Object> result = companiesImportService.confirmImport(orgId, confirmedRows);
        
        return ResponseEntity.ok(result);
    }
    
}
