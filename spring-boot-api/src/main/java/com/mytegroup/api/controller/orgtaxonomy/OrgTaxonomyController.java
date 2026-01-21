package com.mytegroup.api.controller.orgtaxonomy;

import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyDto;
import com.mytegroup.api.entity.core.OrgTaxonomy;
import com.mytegroup.api.service.common.ActorContext;
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
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<OrgTaxonomy> taxonomies = orgTaxonomyService.list(actor, resolvedOrgId);
        
        List<Map<String, Object>> response = taxonomies.stream()
            .map(this::taxonomyToMap)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> put(
            @PathVariable String key,
            @RequestBody @Valid PutOrgTaxonomyDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        OrgTaxonomy taxonomy = orgTaxonomyService.put(key, dto.getValues(), actor, resolvedOrgId);
        
        return ResponseEntity.ok(taxonomyToMap(taxonomy));
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> get(
            @PathVariable String key,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        OrgTaxonomy taxonomy = orgTaxonomyService.get(key, actor, resolvedOrgId);
        
        return ResponseEntity.ok(taxonomyToMap(taxonomy));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> delete(
            @PathVariable String key,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        orgTaxonomyService.delete(key, actor, resolvedOrgId);
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    private Map<String, Object> taxonomyToMap(OrgTaxonomy taxonomy) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", taxonomy.getId());
        map.put("key", taxonomy.getKey());
        map.put("values", taxonomy.getValues());
        map.put("orgId", taxonomy.getOrganization() != null ? taxonomy.getOrganization().getId() : null);
        map.put("createdAt", taxonomy.getCreatedAt());
        map.put("updatedAt", taxonomy.getUpdatedAt());
        return map;
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
