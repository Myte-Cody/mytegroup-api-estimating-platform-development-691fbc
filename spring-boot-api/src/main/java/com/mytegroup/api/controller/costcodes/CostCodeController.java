package com.mytegroup.api.controller.costcodes;

import com.mytegroup.api.dto.costcodes.*;
import com.mytegroup.api.entity.projects.CostCode;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.costcodes.CostCodesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/cost-codes")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CostCodeController {

    private final CostCodesService costCodesService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Page<CostCode> costCodes = costCodesService.list(actor, resolvedOrgId, q, activeOnly, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", costCodes.getContent().stream().map(this::costCodeToMap).toList());
        response.put("total", costCodes.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @RequestBody @Valid CreateCostCodeDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CostCode costCode = new CostCode();
        costCode.setCode(dto.getCode());
        costCode.setLabel(dto.getLabel());
        costCode.setDescription(dto.getDescription());
        costCode.setCategory(dto.getCategory());
        costCode.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        CostCode savedCostCode = costCodesService.create(costCode, actor, resolvedOrgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(costCodeToMap(savedCostCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CostCode costCode = costCodesService.getById(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(costCodeToMap(costCode));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCostCodeDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CostCode costCodeUpdates = new CostCode();
        costCodeUpdates.setCode(dto.getCode());
        costCodeUpdates.setLabel(dto.getLabel());
        costCodeUpdates.setDescription(dto.getDescription());
        costCodeUpdates.setCategory(dto.getCategory());
        costCodeUpdates.setIsActive(dto.getIsActive());
        
        CostCode updatedCostCode = costCodesService.update(id, costCodeUpdates, actor, resolvedOrgId);
        
        return ResponseEntity.ok(costCodeToMap(updatedCostCode));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> toggle(
            @PathVariable Long id,
            @RequestBody @Valid ToggleCostCodeDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        CostCode toggledCostCode = costCodesService.toggle(id, dto.getIsActive(), actor, resolvedOrgId);
        
        return ResponseEntity.ok(costCodeToMap(toggledCostCode));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> bulk(
            @RequestBody @Valid BulkCostCodesDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<CostCode> costCodes = costCodesService.bulkCreate(dto.getCostCodes(), actor, resolvedOrgId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("created", costCodes.size());
        response.put("data", costCodes.stream().map(this::costCodeToMap).toList());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seed")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> seed(
            @RequestBody @Valid SeedCostCodesDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<CostCode> seededCostCodes = costCodesService.seedDefaults(actor, resolvedOrgId, dto.getTemplateId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("seeded", seededCostCodes.size());
        response.put("data", seededCostCodes.stream().map(this::costCodeToMap).toList());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> importPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> preview = costCodesService.importPreview(file, actor, resolvedOrgId);
        
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/import/commit")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> importCommit(
            @RequestBody @Valid CostCodeImportCommitDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> result = costCodesService.importCommit(dto.getPreviewId(), dto.getConfirmedRows(), actor, resolvedOrgId);
        
        return ResponseEntity.ok(result);
    }
    
    // Helper methods
    
    private Map<String, Object> costCodeToMap(CostCode costCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", costCode.getId());
        map.put("code", costCode.getCode());
        map.put("label", costCode.getLabel());
        map.put("description", costCode.getDescription());
        map.put("category", costCode.getCategory());
        map.put("isActive", costCode.getIsActive());
        map.put("piiStripped", costCode.getPiiStripped());
        map.put("legalHold", costCode.getLegalHold());
        map.put("orgId", costCode.getOrganization() != null ? costCode.getOrganization().getId() : null);
        map.put("createdAt", costCode.getCreatedAt());
        map.put("updatedAt", costCode.getUpdatedAt());
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
