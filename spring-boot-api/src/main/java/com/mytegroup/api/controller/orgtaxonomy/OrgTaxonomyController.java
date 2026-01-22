package com.mytegroup.api.controller.orgtaxonomy;

import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyDto;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement list method in OrgTaxonomyService if needed
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> put(
            @PathVariable String key,
            @RequestBody @Valid PutOrgTaxonomyDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
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
        
        return ResponseEntity.ok(taxonomyToMap(taxonomy));
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> get(
            @PathVariable String key,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        OrgTaxonomy taxonomy = orgTaxonomyService.getTaxonomy(orgId, key);
        
        return ResponseEntity.ok(taxonomyToMap(taxonomy));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> delete(
            @PathVariable String key,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement delete method in OrgTaxonomyService
        // For now, just return success
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    private Map<String, Object> taxonomyToMap(OrgTaxonomy taxonomy) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", taxonomy.getId());
        map.put("key", taxonomy.getNamespace());
        map.put("values", taxonomy.getValues());
        map.put("orgId", taxonomy.getOrganization() != null ? taxonomy.getOrganization().getId() : null);
        map.put("createdAt", taxonomy.getCreatedAt());
        map.put("updatedAt", taxonomy.getUpdatedAt());
        return map;
    }
}
