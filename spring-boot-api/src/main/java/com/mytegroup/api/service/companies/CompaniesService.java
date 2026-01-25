package com.mytegroup.api.service.companies;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.companies.CompanyRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for company management.
 * Handles CRUD operations, company types, tags, and relationships.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompaniesService {
    
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Creates a new company
     */
    @Transactional
    public Company create(Company company, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        Organization org = authHelper.validateOrg(orgId);
        company.setOrganization(org);
        
        // Normalize name and check for collision
        String normalizedName = validationHelper.normalizeName(company.getName());
        if (companyRepository.findByOrganization_IdAndNormalizedName(org.getId(), normalizedName)
            .filter(c -> c.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Company already exists for this organization");
        }
        company.setNormalizedName(normalizedName);
        
        // Check external ID uniqueness
        if (company.getExternalId() != null && !company.getExternalId().trim().isEmpty()) {
            if (companyRepository.findByOrganization_IdAndExternalId(org.getId(), company.getExternalId())
                .filter(c -> c.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Company externalId already exists for this organization");
            }
        }
        
        // Normalize keys
        if (company.getCompanyTypeKeys() != null) {
            company.setCompanyTypeKeys(validationHelper.normalizeKeys(company.getCompanyTypeKeys()));
        }
        if (company.getTagKeys() != null) {
            company.setTagKeys(validationHelper.normalizeKeys(company.getTagKeys()));
        }
        
        // Set defaults
        if (company.getPiiStripped() == null) {
            company.setPiiStripped(false);
        }
        if (company.getLegalHold() == null) {
            company.setLegalHold(false);
        }
        
        Company savedCompany = companyRepository.save(company);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", savedCompany.getName());
        
        auditLogService.log(
            "company.created",
            orgId,
            null, // userId will be set when sessions are implemented
            "Company",
            savedCompany.getId().toString(),
            metadata
        );
        
        return savedCompany;
    }
    
    /**
     * Lists companies for an organization
     */
    @Transactional(readOnly = true)
    public Page<Company> list(String orgId, String search, Boolean includeArchived, 
                              String type, String tag, int page, int limit) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Long orgIdLong = Long.parseLong(orgId);
        Specification<Company> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("organization").get("id"), orgIdLong)
        );
        
        if (includeArchived == null || !includeArchived) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("archivedAt")));
        }
        
        if (search != null && !search.trim().isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("name")), searchPattern),
                    cb.like(cb.lower(root.get("normalizedName")), searchPattern)
                )
            );
        }
        
        if (type != null && !type.trim().isEmpty()) {
            String typeKey = validationHelper.normalizeKey(type);
            spec = spec.and((root, query, cb) -> 
                cb.isMember(typeKey, root.get("companyTypeKeys"))
            );
        }
        
        if (tag != null && !tag.trim().isEmpty()) {
            String tagKey = validationHelper.normalizeKey(tag);
            spec = spec.and((root, query, cb) -> 
                cb.isMember(tagKey, root.get("tagKeys"))
            );
        }
        
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit);
        
        return companyRepository.findAll(spec, pageable);
    }
    
    /**
     * Gets a company by ID
     */
    @Transactional(readOnly = true)
    public Company getById(Long id, String orgId, boolean includeArchived) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        
        if (orgId != null && (company.getOrganization() == null || 
            !company.getOrganization().getId().toString().equals(orgId))) {
            throw new ForbiddenException("Cannot access companies outside your organization");
        }
        
        if (!includeArchived && company.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Company not found");
        }
        
        return company;
    }
    
    /**
     * Updates a company
     */
    @Transactional
    public Company update(Long id, Company companyUpdates, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        
        if (company.getOrganization() == null || 
            !company.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access companies outside your organization");
        }
        
        if (company.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Company archived");
        }
        
        authHelper.ensureNotOnLegalHold(company, "update");
        
        // Update name with normalization and collision check
        if (companyUpdates.getName() != null && !companyUpdates.getName().equals(company.getName())) {
            String normalizedName = validationHelper.normalizeName(companyUpdates.getName());
            if (companyRepository.findByOrganization_IdAndNormalizedName(company.getOrganization().getId(), normalizedName)
                .filter(c -> !c.getId().equals(id) && c.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Company already exists for this organization");
            }
            company.setName(companyUpdates.getName());
            company.setNormalizedName(normalizedName);
        }
        
        // Update external ID with uniqueness check
        if (companyUpdates.getExternalId() != null) {
            String externalId = companyUpdates.getExternalId().trim().isEmpty() ? null : companyUpdates.getExternalId();
            if (externalId != null && !externalId.equals(company.getExternalId())) {
                if (companyRepository.findByOrganization_IdAndExternalId(company.getOrganization().getId(), externalId)
                    .filter(c -> !c.getId().equals(id) && c.getArchivedAt() == null)
                    .isPresent()) {
                    throw new ConflictException("Company externalId already exists for this organization");
                }
            }
            company.setExternalId(externalId);
        }
        
        // Update other fields
        if (companyUpdates.getWebsite() != null) {
            company.setWebsite(companyUpdates.getWebsite());
        }
        if (companyUpdates.getMainEmail() != null) {
            company.setMainEmail(validationHelper.normalizeEmail(companyUpdates.getMainEmail()));
        }
        if (companyUpdates.getMainPhone() != null) {
            company.setMainPhone(validationHelper.normalizePhoneE164(companyUpdates.getMainPhone()));
        }
        if (companyUpdates.getCompanyTypeKeys() != null) {
            company.setCompanyTypeKeys(validationHelper.normalizeKeys(companyUpdates.getCompanyTypeKeys()));
        }
        if (companyUpdates.getTagKeys() != null) {
            company.setTagKeys(validationHelper.normalizeKeys(companyUpdates.getTagKeys()));
        }
        if (companyUpdates.getRating() != null) {
            company.setRating(companyUpdates.getRating());
        }
        if (companyUpdates.getNotes() != null) {
            company.setNotes(companyUpdates.getNotes());
        }
        
        Company savedCompany = companyRepository.save(company);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", Map.of("updated", true));
        
        auditLogService.log(
            "company.updated",
            orgId,
            null, // userId will be set when sessions are implemented
            "Company",
            savedCompany.getId().toString(),
            metadata
        );
        
        return savedCompany;
    }
    
    /**
     * Archives a company
     */
    @Transactional
    public Company archive(Long id, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        
        if (company.getOrganization() == null || 
            !company.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access companies outside your organization");
        }
        
        authHelper.ensureNotOnLegalHold(company, "archive");
        
        if (company.getArchivedAt() != null) {
            return company;
        }
        
        company.setArchivedAt(LocalDateTime.now());
        Company savedCompany = companyRepository.save(company);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedCompany.getArchivedAt());
        
        auditLogService.log(
            "company.archived",
            orgId,
            null, // userId will be set when sessions are implemented
            "Company",
            savedCompany.getId().toString(),
            metadata
        );
        
        return savedCompany;
    }
    
    /**
     * Unarchives a company
     */
    @Transactional
    public Company unarchive(Long id, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        
        if (company.getOrganization() == null || 
            !company.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access companies outside your organization");
        }
        
        authHelper.ensureNotOnLegalHold(company, "unarchive");
        
        if (company.getArchivedAt() == null) {
            return company;
        }
        
        // Check for name collision when unarchiving
        String normalizedName = company.getNormalizedName() != null 
            ? company.getNormalizedName() 
            : validationHelper.normalizeName(company.getName());
        if (companyRepository.findByOrganization_IdAndNormalizedName(company.getOrganization().getId(), normalizedName)
            .filter(c -> !c.getId().equals(id) && c.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Company already exists for this organization");
        }
        
        // Check external ID collision
        if (company.getExternalId() != null) {
            if (companyRepository.findByOrganization_IdAndExternalId(company.getOrganization().getId(), company.getExternalId())
                .filter(c -> !c.getId().equals(id) && c.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Company externalId already exists for this organization");
            }
        }
        
        company.setArchivedAt(null);
        Company savedCompany = companyRepository.save(company);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedCompany.getArchivedAt());
        
        auditLogService.log(
            "company.unarchived",
            orgId,
            null, // userId will be set when sessions are implemented
            "Company",
            savedCompany.getId().toString(),
            metadata
        );
        
        return savedCompany;
    }
}

