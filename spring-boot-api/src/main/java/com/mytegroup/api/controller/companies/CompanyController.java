package com.mytegroup.api.controller.companies;

import com.mytegroup.api.dto.companies.*;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.mapper.companies.CompanyMapper;
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
    private final ServiceAuthorizationHelper authHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) {
            return Map.of("error", "orgId is required");
        }
        
        Page<Company> companies = companiesService.list(orgId, search, includeArchived, type, tag, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", companies.getContent().stream().map(this::companyToMap).toList());
        response.put("total", companies.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return response;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> create(
            @RequestBody @Valid CreateCompanyDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return Map.of("error", "orgId is required");
        }
        
        // Get organization for mapper
        Organization organization = authHelper.validateOrg(orgId);
        Company company = companyMapper.toEntity(dto, organization);
        
        Company savedCompany = companiesService.create(company, orgId);
        
        return companyToMap(savedCompany);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        Company company = companiesService.getById(id, orgId, includeArchived);
        
        return companyToMap(company);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompanyDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return Map.of("error", "orgId is required");
        }
        
        // Create a Company object with updates using mapper
        Company companyUpdates = new Company();
        companyMapper.updateEntity(companyUpdates, dto);
        
        Company updatedCompany = companiesService.update(id, companyUpdates, orgId);
        
        return companyToMap(updatedCompany);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return Map.of("error", "orgId is required");
        }
        
        Company archivedCompany = companiesService.archive(id, orgId);
        
        return companyToMap(archivedCompany);
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) {
            return Map.of("error", "orgId is required");
        }
        
        Company unarchivedCompany = companiesService.unarchive(id, orgId);
        
        return companyToMap(unarchivedCompany);
    }
    
    // Helper methods
    
    private Map<String, Object> companyToMap(Company company) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", company.getId());
        map.put("name", company.getName());
        map.put("normalizedName", company.getNormalizedName());
        map.put("externalId", company.getExternalId());
        map.put("website", company.getWebsite());
        map.put("mainEmail", company.getMainEmail());
        map.put("mainPhone", company.getMainPhone());
        map.put("companyTypeKeys", company.getCompanyTypeKeys());
        map.put("tagKeys", company.getTagKeys());
        map.put("rating", company.getRating());
        map.put("notes", company.getNotes());
        map.put("piiStripped", company.getPiiStripped());
        map.put("legalHold", company.getLegalHold());
        map.put("archivedAt", company.getArchivedAt());
        map.put("orgId", company.getOrganization() != null ? company.getOrganization().getId() : null);
        map.put("createdAt", company.getCreatedAt());
        map.put("updatedAt", company.getUpdatedAt());
        return map;
    }
    
}
