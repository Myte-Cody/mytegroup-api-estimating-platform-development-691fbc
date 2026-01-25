package com.mytegroup.api.service.companylocations;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.companies.CompanyLocationRepository;
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
import java.util.Map;

/**
 * Service for company location management.
 * Handles CRUD operations for company locations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyLocationsService {
    
    private final CompanyLocationRepository companyLocationRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Creates a new company location
     */
    @Transactional
    public CompanyLocation create(CompanyLocation location, String orgId) {
        if (orgId == null) {
            throw new com.mytegroup.api.exception.BadRequestException("orgId is required");
        }
        Organization org = authHelper.validateOrg(orgId);
        location.setOrganization(org);
        
        Long orgIdLong = Long.parseLong(orgId);
        
        // Validate company exists
        if (location.getCompany() == null || location.getCompany().getId() == null) {
            throw new ResourceNotFoundException("Company is required");
        }
        Company company = companyRepository.findById(location.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Company not found or archived"));
        if (company.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Company not found or archived");
        }
        location.setCompany(company);
        
        // Normalize name and check for collision
        String normalizedName = validationHelper.normalizeName(location.getName());
        if (companyLocationRepository.findByOrganization_IdAndCompanyIdAndNormalizedName(
            orgIdLong, company.getId(), normalizedName)
            .filter(l -> l.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Company location already exists for this company");
        }
        location.setNormalizedName(normalizedName);
        
        // Check external ID uniqueness
        if (location.getExternalId() != null && !location.getExternalId().trim().isEmpty()) {
            if (companyLocationRepository.findByOrganization_IdAndCompanyIdAndExternalId(
                orgIdLong, company.getId(), location.getExternalId())
                .filter(l -> l.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Company location externalId already exists for this company");
            }
        }
        
        // Normalize tag keys
        if (location.getTagKeys() != null) {
            location.setTagKeys(validationHelper.normalizeKeys(location.getTagKeys()));
        }
        
        // Set defaults
        if (location.getPiiStripped() == null) {
            location.setPiiStripped(false);
        }
        if (location.getLegalHold() == null) {
            location.setLegalHold(false);
        }
        
        CompanyLocation savedLocation = companyLocationRepository.save(location);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("companyId", company.getId().toString());
        metadata.put("name", savedLocation.getName());
        
        auditLogService.log(
            "company_location.created",
            orgId,
            null, // userId will be set when sessions are implemented
            "CompanyLocation",
            savedLocation.getId().toString(),
            metadata
        );
        
        return savedLocation;
    }
    
    /**
     * Lists company locations for an organization
     */
    @Transactional(readOnly = true)
    public Page<CompanyLocation> list(String orgId, Long companyId, String search, 
                                      String tag, Boolean includeArchived, int page, int limit) {
        if (orgId == null) {
            throw new com.mytegroup.api.exception.BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Long orgIdLong = Long.parseLong(orgId);
        Specification<CompanyLocation> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("organization").get("id"), orgIdLong)
        );
        
        if (companyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("company").get("id"), companyId));
        }
        
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
        
        if (tag != null && !tag.trim().isEmpty()) {
            String tagKey = validationHelper.normalizeKey(tag);
            spec = spec.and((root, query, cb) -> 
                cb.isMember(tagKey, root.get("tagKeys"))
            );
        }
        
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit);
        
        // Note: CompanyLocationRepository needs JpaSpecificationExecutor
        return companyLocationRepository.findAll(spec, pageable);
    }
    
    /**
     * Gets a company location by ID
     */
    @Transactional(readOnly = true)
    public CompanyLocation getById(Long id, String orgId, boolean includeArchived) {
        CompanyLocation location = companyLocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company location not found"));
        
        if (orgId != null && (location.getOrganization() == null || 
            !location.getOrganization().getId().toString().equals(orgId))) {
            throw new ResourceNotFoundException("Company location not found");
        }
        
        if (!includeArchived && location.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Company location not found");
        }
        
        return location;
    }
    
    /**
     * Updates a company location
     */
    @Transactional
    public CompanyLocation update(Long id, CompanyLocation locationUpdates, String orgId) {
        if (orgId == null) {
            throw new com.mytegroup.api.exception.BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        CompanyLocation location = companyLocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company location not found"));
        
        if (location.getOrganization() == null || 
            !location.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Company location not found");
        }
        
        if (location.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Company location archived");
        }
        
        authHelper.ensureNotOnLegalHold(location, "update");
        
        Long orgIdLong = Long.parseLong(orgId);
        Long companyId = location.getCompany().getId();
        
        // Update name with normalization and collision check
        if (locationUpdates.getName() != null && !locationUpdates.getName().equals(location.getName())) {
            String normalizedName = validationHelper.normalizeName(locationUpdates.getName());
            if (companyLocationRepository.findByOrganization_IdAndCompanyIdAndNormalizedName(orgIdLong, companyId, normalizedName)
                .filter(l -> !l.getId().equals(id) && l.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Company location already exists for this company");
            }
            location.setName(locationUpdates.getName());
            location.setNormalizedName(normalizedName);
        }
        
        // Update external ID with uniqueness check
        if (locationUpdates.getExternalId() != null) {
            String externalId = locationUpdates.getExternalId().trim().isEmpty() ? null : locationUpdates.getExternalId();
            if (externalId != null && !externalId.equals(location.getExternalId())) {
                if (companyLocationRepository.findByOrganization_IdAndCompanyIdAndExternalId(orgIdLong, companyId, externalId)
                    .filter(l -> !l.getId().equals(id) && l.getArchivedAt() == null)
                    .isPresent()) {
                    throw new ConflictException("Company location externalId already exists for this company");
                }
            }
            location.setExternalId(externalId);
        }
        
        // Update other fields
        if (locationUpdates.getTimezone() != null) {
            location.setTimezone(locationUpdates.getTimezone());
        }
        if (locationUpdates.getEmail() != null) {
            location.setEmail(validationHelper.normalizeEmail(locationUpdates.getEmail()));
        }
        if (locationUpdates.getPhone() != null) {
            location.setPhone(validationHelper.normalizePhoneE164(locationUpdates.getPhone()));
        }
        if (locationUpdates.getAddressLine1() != null) {
            location.setAddressLine1(locationUpdates.getAddressLine1());
        }
        if (locationUpdates.getAddressLine2() != null) {
            location.setAddressLine2(locationUpdates.getAddressLine2());
        }
        if (locationUpdates.getCity() != null) {
            location.setCity(locationUpdates.getCity());
        }
        if (locationUpdates.getRegion() != null) {
            location.setRegion(locationUpdates.getRegion());
        }
        if (locationUpdates.getPostal() != null) {
            location.setPostal(locationUpdates.getPostal());
        }
        if (locationUpdates.getCountry() != null) {
            location.setCountry(locationUpdates.getCountry());
        }
        if (locationUpdates.getTagKeys() != null) {
            location.setTagKeys(validationHelper.normalizeKeys(locationUpdates.getTagKeys()));
        }
        if (locationUpdates.getNotes() != null) {
            location.setNotes(locationUpdates.getNotes());
        }
        
        CompanyLocation savedLocation = companyLocationRepository.save(location);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", Map.of("updated", true));
        
        auditLogService.log(
            "company_location.updated",
            orgId,
            null, // userId will be set when sessions are implemented
            "CompanyLocation",
            savedLocation.getId().toString(),
            metadata
        );
        
        return savedLocation;
    }
    
    /**
     * Archives a company location
     */
    @Transactional
    public CompanyLocation archive(Long id, String orgId) {
        if (orgId == null) {
            throw new com.mytegroup.api.exception.BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        CompanyLocation location = companyLocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company location not found"));
        
        if (location.getOrganization() == null || 
            !location.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Company location not found");
        }
        
        authHelper.ensureNotOnLegalHold(location, "archive");
        
        if (location.getArchivedAt() != null) {
            return location;
        }
        
        location.setArchivedAt(LocalDateTime.now());
        CompanyLocation savedLocation = companyLocationRepository.save(location);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedLocation.getArchivedAt());
        
        auditLogService.log(
            "company_location.archived",
            orgId,
            null, // userId will be set when sessions are implemented
            "CompanyLocation",
            savedLocation.getId().toString(),
            metadata
        );
        
        return savedLocation;
    }
    
    /**
     * Unarchives a company location
     */
    @Transactional
    public CompanyLocation unarchive(Long id, String orgId) {
        if (orgId == null) {
            throw new com.mytegroup.api.exception.BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        CompanyLocation location = companyLocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company location not found"));
        
        if (location.getOrganization() == null || 
            !location.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Company location not found");
        }
        
        authHelper.ensureNotOnLegalHold(location, "unarchive");
        
        if (location.getArchivedAt() == null) {
            return location;
        }
        
        Long orgIdLong = Long.parseLong(orgId);
        Long companyId = location.getCompany().getId();
        
        // Check for name collision when unarchiving
        String normalizedName = location.getNormalizedName() != null 
            ? location.getNormalizedName() 
            : validationHelper.normalizeName(location.getName());
        if (companyLocationRepository.findByOrganization_IdAndCompanyIdAndNormalizedName(orgIdLong, companyId, normalizedName)
            .filter(l -> !l.getId().equals(id) && l.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Company location already exists for this company");
        }
        
        // Check external ID collision
        if (location.getExternalId() != null) {
            if (companyLocationRepository.findByOrganization_IdAndCompanyIdAndExternalId(orgIdLong, companyId, location.getExternalId())
                .filter(l -> !l.getId().equals(id) && l.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Company location externalId already exists for this company");
            }
        }
        
        location.setArchivedAt(null);
        CompanyLocation savedLocation = companyLocationRepository.save(location);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedLocation.getArchivedAt());
        
        auditLogService.log(
            "company_location.unarchived",
            orgId,
            null, // userId will be set when sessions are implemented
            "CompanyLocation",
            savedLocation.getId().toString(),
            metadata
        );
        
        return savedLocation;
    }
}

