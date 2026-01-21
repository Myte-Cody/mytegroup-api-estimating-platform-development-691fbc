package com.mytegroup.api.controller.companies;

import com.mytegroup.api.dto.companies.*;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.companies.CompaniesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Page<Company> companies = companiesService.list(actor, resolvedOrgId, search, includeArchived, type, tag, page, limit);
        
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
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Company company = new Company();
        company.setName(dto.getName());
        company.setExternalId(dto.getExternalId());
        company.setWebsite(dto.getWebsite());
        company.setMainEmail(dto.getMainEmail());
        company.setMainPhone(dto.getMainPhone());
        company.setCompanyTypeKeys(dto.getCompanyTypeKeys());
        company.setTagKeys(dto.getTagKeys());
        company.setRating(dto.getRating());
        company.setNotes(dto.getNotes());
        
        Company savedCompany = companiesService.create(company, actor, resolvedOrgId);
        
        return companyToMap(savedCompany);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Company company = companiesService.getById(id, actor, resolvedOrgId, includeArchived);
        
        return companyToMap(company);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompanyDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Company companyUpdates = new Company();
        companyUpdates.setName(dto.getName());
        companyUpdates.setExternalId(dto.getExternalId());
        companyUpdates.setWebsite(dto.getWebsite());
        companyUpdates.setMainEmail(dto.getMainEmail());
        companyUpdates.setMainPhone(dto.getMainPhone());
        companyUpdates.setCompanyTypeKeys(dto.getCompanyTypeKeys());
        companyUpdates.setTagKeys(dto.getTagKeys());
        companyUpdates.setRating(dto.getRating());
        companyUpdates.setNotes(dto.getNotes());
        
        Company updatedCompany = companiesService.update(id, companyUpdates, actor, resolvedOrgId);
        
        return companyToMap(updatedCompany);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Company archivedCompany = companiesService.archive(id, actor, resolvedOrgId);
        
        return companyToMap(archivedCompany);
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Company unarchivedCompany = companiesService.unarchive(id, actor, resolvedOrgId);
        
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
