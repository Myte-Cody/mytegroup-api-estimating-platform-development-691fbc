package com.mytegroup.api.controller.companylocations;

import com.mytegroup.api.dto.companylocations.*;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.companylocations.CompanyLocationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/company-locations")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CompanyLocationController {

    private final CompanyLocationsService companyLocationsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Page<CompanyLocation> locations = companyLocationsService.list(actor, resolvedOrgId, companyId, includeArchived, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", locations.getContent().stream().map(this::locationToMap).toList());
        response.put("total", locations.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @RequestBody @Valid CreateCompanyLocationDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CompanyLocation location = new CompanyLocation();
        location.setName(dto.getName());
        location.setExternalId(dto.getExternalId());
        location.setIsPrimary(dto.getIsPrimary());
        location.setAddressLine1(dto.getAddressLine1());
        location.setAddressLine2(dto.getAddressLine2());
        location.setCity(dto.getCity());
        location.setState(dto.getState());
        location.setPostalCode(dto.getPostalCode());
        location.setCountry(dto.getCountry());
        location.setLatitude(dto.getLatitude());
        location.setLongitude(dto.getLongitude());
        location.setPhone(dto.getPhone());
        location.setEmail(dto.getEmail());
        location.setNotes(dto.getNotes());
        
        CompanyLocation savedLocation = companyLocationsService.create(location, dto.getCompanyId(), actor, resolvedOrgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(locationToMap(savedLocation));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CompanyLocation location = companyLocationsService.getById(id, actor, resolvedOrgId, includeArchived);
        
        return ResponseEntity.ok(locationToMap(location));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompanyLocationDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CompanyLocation locationUpdates = new CompanyLocation();
        locationUpdates.setName(dto.getName());
        locationUpdates.setExternalId(dto.getExternalId());
        locationUpdates.setIsPrimary(dto.getIsPrimary());
        locationUpdates.setAddressLine1(dto.getAddressLine1());
        locationUpdates.setAddressLine2(dto.getAddressLine2());
        locationUpdates.setCity(dto.getCity());
        locationUpdates.setState(dto.getState());
        locationUpdates.setPostalCode(dto.getPostalCode());
        locationUpdates.setCountry(dto.getCountry());
        locationUpdates.setLatitude(dto.getLatitude());
        locationUpdates.setLongitude(dto.getLongitude());
        locationUpdates.setPhone(dto.getPhone());
        locationUpdates.setEmail(dto.getEmail());
        locationUpdates.setNotes(dto.getNotes());
        
        CompanyLocation updatedLocation = companyLocationsService.update(id, locationUpdates, actor, resolvedOrgId);
        
        return ResponseEntity.ok(locationToMap(updatedLocation));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CompanyLocation archivedLocation = companyLocationsService.archive(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(locationToMap(archivedLocation));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CompanyLocation unarchivedLocation = companyLocationsService.unarchive(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(locationToMap(unarchivedLocation));
    }
    
    // Helper methods
    
    private Map<String, Object> locationToMap(CompanyLocation location) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", location.getId());
        map.put("name", location.getName());
        map.put("externalId", location.getExternalId());
        map.put("isPrimary", location.getIsPrimary());
        map.put("addressLine1", location.getAddressLine1());
        map.put("addressLine2", location.getAddressLine2());
        map.put("city", location.getCity());
        map.put("state", location.getState());
        map.put("postalCode", location.getPostalCode());
        map.put("country", location.getCountry());
        map.put("latitude", location.getLatitude());
        map.put("longitude", location.getLongitude());
        map.put("phone", location.getPhone());
        map.put("email", location.getEmail());
        map.put("notes", location.getNotes());
        map.put("companyId", location.getCompany() != null ? location.getCompany().getId() : null);
        map.put("archivedAt", location.getArchivedAt());
        map.put("createdAt", location.getCreatedAt());
        map.put("updatedAt", location.getUpdatedAt());
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
