package com.mytegroup.api.controller.orgtaxonomy;

import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyDto;
import com.mytegroup.api.dto.response.OrgTaxonomyResponseDto;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
import com.mytegroup.api.mapper.response.OrgTaxonomyResponseMapper;
import com.mytegroup.api.service.orgtaxonomy.OrgTaxonomyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/org-taxonomy")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OrgTaxonomyController {

    private final OrgTaxonomyService orgTaxonomyService;
    private final OrgTaxonomyResponseMapper orgTaxonomyResponseMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        // TODO: Implement list method in OrgTaxonomyService if needed
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<OrgTaxonomyResponseDto> put(
            @PathVariable String key,
            @RequestBody @Valid PutOrgTaxonomyDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        // Convert DTO values to OrgTaxonomyValue entities
        List<com.mytegroup.api.entity.organization.embeddable.OrgTaxonomyValue> values = 
            dto.getValues() != null ? dto.getValues().stream()
                .map(v -> {
                    com.mytegroup.api.entity.organization.embeddable.OrgTaxonomyValue value = 
                        new com.mytegroup.api.entity.organization.embeddable.OrgTaxonomyValue();
                    value.setKey(v.key());
                    value.setLabel(v.label());
                    value.setSortOrder(v.sortOrder());
                    value.setColor(v.color());
                    // Convert metadata Map to JSON string if needed
                    if (v.metadata() != null) {
                        try {
                            value.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(v.metadata()));
                        } catch (Exception e) {
                            value.setMetadata(null);
                        }
                    }
                    return value;
                })
                .toList() : List.of();
        OrgTaxonomy taxonomy = orgTaxonomyService.putValues(orgId, key, values);
        
        return ResponseEntity.ok(orgTaxonomyResponseMapper.toDto(taxonomy));
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<OrgTaxonomyResponseDto> get(
            @PathVariable String key,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        OrgTaxonomy taxonomy = orgTaxonomyService.getTaxonomy(orgId, key);
        
        return ResponseEntity.ok(orgTaxonomyResponseMapper.toDto(taxonomy));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> delete(
            @PathVariable String key,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        // TODO: Implement delete method in OrgTaxonomyService
        // For now, just return success
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
}
