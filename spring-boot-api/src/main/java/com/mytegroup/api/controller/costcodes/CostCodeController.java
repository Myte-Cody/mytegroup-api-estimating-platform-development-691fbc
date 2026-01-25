package com.mytegroup.api.controller.costcodes;

import com.mytegroup.api.dto.costcodes.*;
import com.mytegroup.api.dto.response.CostCodeResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.mapper.costcodes.CostCodeMapper;
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
    public ResponseEntity<PaginatedResponseDto<CostCodeResponseDto>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(PaginatedResponseDto.<CostCodeResponseDto>builder()
                    .data(List.of())
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .build());
        }
        
        Page<CostCode> costCodes = costCodesService.list(orgId, q, activeOnly, page, limit);
        
        return ResponseEntity.ok(PaginatedResponseDto.<CostCodeResponseDto>builder()
                .data(costCodes.getContent().stream().map(costCodeMapper::toDto).toList())
                .total(costCodes.getTotalElements())
                .page(page)
                .limit(limit)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<CostCodeResponseDto> create(
            @RequestBody @Valid CreateCostCodeDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        // Get organization for mapper
        Organization organization = authHelper.validateOrg(orgId);
        CostCode costCode = costCodeMapper.toEntity(dto, organization);
        
        CostCode savedCostCode = costCodesService.create(costCode, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(costCodeMapper.toDto(savedCostCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostCodeResponseDto> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        CostCode costCode = costCodesService.getById(id, orgId);
        
        return ResponseEntity.ok(costCodeMapper.toDto(costCode));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<CostCodeResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCostCodeDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        // Create a CostCode object with updates using mapper
        CostCode costCodeUpdates = new CostCode();
        costCodeMapper.updateEntity(costCodeUpdates, dto);
        
        CostCode updatedCostCode = costCodesService.update(id, costCodeUpdates, orgId);
        
        return ResponseEntity.ok(costCodeMapper.toDto(updatedCostCode));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<CostCodeResponseDto> toggle(
            @PathVariable Long id,
            @RequestBody @Valid ToggleCostCodeDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        CostCode toggledCostCode = costCodesService.toggleActive(id, dto.active(), orgId);
        
        return ResponseEntity.ok(costCodeMapper.toDto(toggledCostCode));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> bulk(
            @RequestBody @Valid BulkCostCodesDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
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
            throw new IllegalArgumentException("orgId is required");
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
            throw new IllegalArgumentException("orgId is required");
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
            throw new IllegalArgumentException("orgId is required");
        }
        
        // TODO: Implement importCommit method in service
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("error", "Import commit not yet implemented"));
    }
    
}
