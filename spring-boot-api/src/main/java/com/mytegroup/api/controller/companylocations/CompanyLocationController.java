package com.mytegroup.api.controller.companylocations;

import com.mytegroup.api.dto.companylocations.*;
import com.mytegroup.api.dto.response.CompanyLocationResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.mapper.companylocations.CompanyLocationMapper;
import com.mytegroup.api.mapper.companylocations.CompanyLocationMapper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.companylocations.CompanyLocationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/company-locations")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CompanyLocationController {

    private final CompanyLocationsService companyLocationsService;
    private final CompanyLocationMapper companyLocationMapper;
    private final ServiceAuthorizationHelper authHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PaginatedResponseDto<CompanyLocationResponseDto>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) {
            return ResponseEntity.badRequest().body(PaginatedResponseDto.<CompanyLocationResponseDto>builder()
                    .data(java.util.List.of())
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .build());
        }
        
        Page<CompanyLocation> locations = companyLocationsService.list(orgId, companyId, null, null, includeArchived, page, limit);
        
        return ResponseEntity.ok(PaginatedResponseDto.<CompanyLocationResponseDto>builder()
                .data(locations.getContent().stream().map(companyLocationMapper::toDto).toList())
                .total(locations.getTotalElements())
                .page(page)
                .limit(limit)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<CompanyLocationResponseDto> create(
            @RequestBody @Valid CreateCompanyLocationDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        // Get organization and company for mapper
        Organization organization = authHelper.validateOrg(orgId);
        Company company = new Company();
        company.setId(Long.parseLong(dto.companyId())); // Service will validate this
        
        CompanyLocation location = companyLocationMapper.toEntity(dto, organization, company);
        
        CompanyLocation savedLocation = companyLocationsService.create(location, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(companyLocationMapper.toDto(savedLocation));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<CompanyLocationResponseDto> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        CompanyLocation location = companyLocationsService.getById(id, orgId, includeArchived);
        
        return ResponseEntity.ok(companyLocationMapper.toDto(location));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<CompanyLocationResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompanyLocationDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        // Create a CompanyLocation object with updates using mapper
        CompanyLocation locationUpdates = new CompanyLocation();
        companyLocationMapper.updateEntity(locationUpdates, dto);
        
        CompanyLocation updatedLocation = companyLocationsService.update(id, locationUpdates, orgId);
        
        return ResponseEntity.ok(companyLocationMapper.toDto(updatedLocation));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<CompanyLocationResponseDto> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        CompanyLocation archivedLocation = companyLocationsService.archive(id, orgId);
        
        return ResponseEntity.ok(companyLocationMapper.toDto(archivedLocation));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<CompanyLocationResponseDto> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        CompanyLocation unarchivedLocation = companyLocationsService.unarchive(id, orgId);
        
        return ResponseEntity.ok(companyLocationMapper.toDto(unarchivedLocation));
    }
    
}
