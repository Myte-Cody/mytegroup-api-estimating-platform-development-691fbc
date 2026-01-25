package com.mytegroup.api.controller.offices;

import com.mytegroup.api.dto.offices.*;
import com.mytegroup.api.dto.response.OfficeResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.mapper.offices.OfficeMapper;
import com.mytegroup.api.mapper.offices.OfficeMapper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.offices.OfficesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offices")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OfficeController {

    private final OfficesService officesService;
    private final OfficeMapper officeMapper;
    private final ServiceAuthorizationHelper authHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PaginatedResponseDto<OfficeResponseDto>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(PaginatedResponseDto.<OfficeResponseDto>builder()
                    .data(List.of())
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .build());
        }
        
        List<Office> offices = officesService.list(orgId, includeArchived != null && includeArchived);
        
        return ResponseEntity.ok(PaginatedResponseDto.<OfficeResponseDto>builder()
                .data(offices.stream().map(officeMapper::toDto).toList())
                .total(offices.size())
                .page(page)
                .limit(limit)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<OfficeResponseDto> create(
            @RequestBody @Valid CreateOfficeDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        // Get organization for mapper
        Organization organization = authHelper.validateOrg(orgId);
        Office parent = null;
        if (dto.parentOrgLocationId() != null) {
            // Parent will be validated by service
        }
        
        Office office = officeMapper.toEntity(dto, organization, parent);
        
        Office savedOffice = officesService.create(office, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(officeMapper.toDto(savedOffice));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<OfficeResponseDto> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        Office office = officesService.getById(id, orgId, includeArchived);
        
        return ResponseEntity.ok(officeMapper.toDto(office));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<OfficeResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOfficeDto dto,
            @RequestParam(required = false) String orgId) {
        
        // Create an Office object with updates using mapper
        Office officeUpdates = new Office();
        Office parent = null;
        if (dto.parentOrgLocationId() != null) {
            // Parent will be handled by service
        }
        officeMapper.updateEntity(officeUpdates, dto, parent);
        
        Office updatedOffice = officesService.update(id, officeUpdates, orgId);
        
        return ResponseEntity.ok(officeMapper.toDto(updatedOffice));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<OfficeResponseDto> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        Office archivedOffice = officesService.archive(id, orgId);
        
        return ResponseEntity.ok(officeMapper.toDto(archivedOffice));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<OfficeResponseDto> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        Office unarchivedOffice = officesService.unarchive(id, orgId);
        
        return ResponseEntity.ok(officeMapper.toDto(unarchivedOffice));
    }
    
}
