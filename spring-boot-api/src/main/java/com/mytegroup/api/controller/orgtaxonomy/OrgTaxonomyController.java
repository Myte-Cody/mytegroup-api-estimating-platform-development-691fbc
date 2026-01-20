package com.mytegroup.api.controller.orgtaxonomy;

import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/org-taxonomy")
@PreAuthorize("isAuthenticated()")
public class OrgTaxonomyController {

    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> put(@PathVariable String key, @RequestBody @Valid PutOrgTaxonomyDto dto) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> get(@PathVariable String key) {
        return ResponseEntity.ok().build();
    }
}
