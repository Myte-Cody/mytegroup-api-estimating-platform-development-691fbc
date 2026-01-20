package com.mytegroup.api.controller.companies;

import com.mytegroup.api.dto.companies.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Companies controller.
 * Endpoints:
 * - GET /companies - List companies (Admin+)
 * - POST /companies - Create company (Admin+)
 * - GET /companies/:id - Get company (Admin+)
 * - PATCH /companies/:id - Update company (Admin+)
 * - POST /companies/:id/archive - Archive company (Admin+)
 * - POST /companies/:id/unarchive - Unarchive company (Admin+)
 */
@RestController
@RequestMapping("/api/companies")
@PreAuthorize("isAuthenticated()")
public class CompanyController {

    // TODO: Inject CompanyService, CompanyMapper

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@ModelAttribute ListCompaniesQueryDto query) {
        // TODO: Implement list companies logic
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(@RequestBody @Valid CreateCompanyDto dto) {
        // TODO: Implement create company logic
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(@PathVariable String id) {
        // TODO: Implement get company by id logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody @Valid UpdateCompanyDto dto) {
        // TODO: Implement update company logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(@PathVariable String id) {
        // TODO: Implement archive company logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(@PathVariable String id) {
        // TODO: Implement unarchive company logic
        return ResponseEntity.ok().build();
    }
}

