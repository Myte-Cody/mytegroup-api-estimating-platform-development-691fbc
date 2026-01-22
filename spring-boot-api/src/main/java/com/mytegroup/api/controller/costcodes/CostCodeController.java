package com.mytegroup.api.controller.costcodes;

import com.mytegroup.api.dto.costcodes.*;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.mapper.costcodes.CostCodeMapper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.costcodes.CostCodesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final CostCodeMapper costCodeMapper;
    private final ServiceAuthorizationHelper authHelper;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        Page<CostCode> costCodes = costCodesService.list(orgId, q, activeOnly, page, limit);
        
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
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // Get organization for mapper
        Organization organization = authHelper.validateOrg(orgId);
        CostCode costCode = costCodeMapper.toEntity(dto, organization);
        
        CostCode savedCostCode = costCodesService.create(costCode, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(costCodeToMap(savedCostCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        CostCode costCode = costCodesService.getById(id, orgId);
        
        return ResponseEntity.ok(costCodeToMap(costCode));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCostCodeDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // Create a CostCode object with updates using mapper
        CostCode costCodeUpdates = new CostCode();
        costCodeMapper.updateEntity(costCodeUpdates, dto);
        
        CostCode updatedCostCode = costCodesService.update(id, costCodeUpdates, orgId);
        
        return ResponseEntity.ok(costCodeToMap(updatedCostCode));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> toggle(
            @PathVariable Long id,
            @RequestBody @Valid ToggleCostCodeDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        CostCode toggledCostCode = costCodesService.toggleActive(id, dto.active(), orgId);
        
        return ResponseEntity.ok(costCodeToMap(toggledCostCode));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> bulk(
            @RequestBody @Valid BulkCostCodesDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // TODO: Implement bulkCreate method in service
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("error", "Bulk create not yet implemented"));
    }

    @PostMapping("/seed")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> seed(
            @RequestBody @Valid SeedCostCodesDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // TODO: Implement seedDefaults method in service
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("error", "Seed defaults not yet implemented"));
    }

    @PostMapping("/import/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> importPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // TODO: Implement importPreview method in service
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("error", "Import preview not yet implemented"));
    }

    @PostMapping("/import/commit")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> importCommit(
            @RequestBody @Valid CostCodeImportCommitDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required"));
        }
        
        // TODO: Implement importCommit method in service
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("error", "Import commit not yet implemented"));
    }
    
    // Helper methods
    
    private Map<String, Object> costCodeToMap(CostCode costCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", costCode.getId());
        map.put("code", costCode.getCode());
        map.put("description", costCode.getDescription());
        map.put("category", costCode.getCategory());
        map.put("active", costCode.getActive());
        map.put("isUsed", costCode.getIsUsed());
        map.put("orgId", costCode.getOrganization() != null ? costCode.getOrganization().getId() : null);
        map.put("createdAt", costCode.getCreatedAt());
        map.put("updatedAt", costCode.getUpdatedAt());
        return map;
    }
    
}
