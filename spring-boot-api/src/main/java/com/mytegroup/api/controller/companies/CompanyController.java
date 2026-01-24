package com.mytegroup.api.controller.companies;

import com.mytegroup.api.dto.companies.*;
import com.mytegroup.api.dto.response.CompanyResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.mapper.companies.CompanyMapper;
import com.mytegroup.api.mapper.response.CompanyResponseMapper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.companies.CompaniesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Companies controller.
 * Endpoints:
 * - GET /companies - List companies (Admin+)
 * - POST /companies - Create company (Admin+) -> 201
 * - GET /companies/:id - Get company (Admin+)
 * - PATCH /companies/:id - Update company (Admin+)
 * - POST /companies/:id/archive - Archive company (Admin+)
 * - POST /companies/:id/unarchive - Unarchive company (Admin+)
 */
@RestController
@RequestMapping("/api/companies")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CompanyController {

    private final CompaniesService companiesService;
    private final CompanyMapper companyMapper;
    private final CompanyResponseMapper companyResponseMapper;
    private final ServiceAuthorizationHelper authHelper;
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public PaginatedResponseDto<CompanyResponseDto> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) {
            return PaginatedResponseDto.<CompanyResponseDto>builder()
                    .data(java.util.List.of())
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .build();
        }
        
        Page<Company> companies = companiesService.list(orgId, search, includeArchived, type, tag, page, limit);
        
        return PaginatedResponseDto.<CompanyResponseDto>builder()
                .data(companies.getContent().stream().map(companyResponseMapper::toDto).toList())
                .total(companies.getTotalElements())
                .page(page)
                .limit(limit)
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CompanyResponseDto create(
            @RequestBody @Valid CreateCompanyDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        // Get organization for mapper
        Organization organization = authHelper.validateOrg(orgId);
        Company company = companyMapper.toEntity(dto, organization);
        
        Company savedCompany = companiesService.create(company, orgId);
        
        return companyResponseMapper.toDto(savedCompany);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CompanyResponseDto getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        Company company = companiesService.getById(id, orgId, includeArchived);
        
        return companyResponseMapper.toDto(company);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CompanyResponseDto update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompanyDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        // Create a Company object with updates using mapper
        Company companyUpdates = new Company();
        companyMapper.updateEntity(companyUpdates, dto);
        
        Company updatedCompany = companiesService.update(id, companyUpdates, orgId);
        
        return companyResponseMapper.toDto(updatedCompany);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CompanyResponseDto archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        Company archivedCompany = companiesService.archive(id, orgId);
        
        return companyResponseMapper.toDto(archivedCompany);
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CompanyResponseDto unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        
        Company unarchivedCompany = companiesService.unarchive(id, orgId);
        
        return companyResponseMapper.toDto(unarchivedCompany);
    }
    
}

