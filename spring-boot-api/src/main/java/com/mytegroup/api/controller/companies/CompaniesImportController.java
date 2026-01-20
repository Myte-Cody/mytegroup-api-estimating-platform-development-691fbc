package com.mytegroup.api.controller.companies;

import com.mytegroup.api.dto.companies.CompaniesImportConfirmDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/companies/import/v1")
@PreAuthorize("isAuthenticated()")
public class CompaniesImportController {

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> confirm(@RequestBody @Valid CompaniesImportConfirmDto dto) {
        return ResponseEntity.ok().build();
    }
}

