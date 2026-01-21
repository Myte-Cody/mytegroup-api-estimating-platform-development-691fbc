package com.mytegroup.api.controller.offices;

import com.mytegroup.api.dto.offices.*;
import com.mytegroup.api.entity.core.Office;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.offices.OfficesService;
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
@RequestMapping("/api/offices")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OfficeController {

    private final OfficesService officesService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Page<Office> offices = officesService.list(actor, resolvedOrgId, includeArchived, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", offices.getContent().stream().map(this::officeToMap).toList());
        response.put("total", offices.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @RequestBody @Valid CreateOfficeDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Office office = new Office();
        office.setName(dto.getName());
        office.setExternalId(dto.getExternalId());
        office.setAddressLine1(dto.getAddressLine1());
        office.setAddressLine2(dto.getAddressLine2());
        office.setCity(dto.getCity());
        office.setState(dto.getState());
        office.setPostalCode(dto.getPostalCode());
        office.setCountry(dto.getCountry());
        office.setPhone(dto.getPhone());
        office.setEmail(dto.getEmail());
        office.setNotes(dto.getNotes());
        
        Office savedOffice = officesService.create(office, actor, resolvedOrgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(officeToMap(savedOffice));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Office office = officesService.getById(id, actor, resolvedOrgId, includeArchived);
        
        return ResponseEntity.ok(officeToMap(office));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOfficeDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Office officeUpdates = new Office();
        officeUpdates.setName(dto.getName());
        officeUpdates.setExternalId(dto.getExternalId());
        officeUpdates.setAddressLine1(dto.getAddressLine1());
        officeUpdates.setAddressLine2(dto.getAddressLine2());
        officeUpdates.setCity(dto.getCity());
        officeUpdates.setState(dto.getState());
        officeUpdates.setPostalCode(dto.getPostalCode());
        officeUpdates.setCountry(dto.getCountry());
        officeUpdates.setPhone(dto.getPhone());
        officeUpdates.setEmail(dto.getEmail());
        officeUpdates.setNotes(dto.getNotes());
        
        Office updatedOffice = officesService.update(id, officeUpdates, actor, resolvedOrgId);
        
        return ResponseEntity.ok(officeToMap(updatedOffice));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Office archivedOffice = officesService.archive(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(officeToMap(archivedOffice));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Office unarchivedOffice = officesService.unarchive(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(officeToMap(unarchivedOffice));
    }
    
    // Helper methods
    
    private Map<String, Object> officeToMap(Office office) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", office.getId());
        map.put("name", office.getName());
        map.put("externalId", office.getExternalId());
        map.put("addressLine1", office.getAddressLine1());
        map.put("addressLine2", office.getAddressLine2());
        map.put("city", office.getCity());
        map.put("state", office.getState());
        map.put("postalCode", office.getPostalCode());
        map.put("country", office.getCountry());
        map.put("phone", office.getPhone());
        map.put("email", office.getEmail());
        map.put("notes", office.getNotes());
        map.put("piiStripped", office.getPiiStripped());
        map.put("legalHold", office.getLegalHold());
        map.put("archivedAt", office.getArchivedAt());
        map.put("orgId", office.getOrganization() != null ? office.getOrganization().getId() : null);
        map.put("createdAt", office.getCreatedAt());
        map.put("updatedAt", office.getUpdatedAt());
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
