package com.mytegroup.api.service.offices;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.organization.OfficeRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for office management.
 * Handles CRUD operations for organizational offices/locations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfficesService {
    
    private final OfficeRepository officeRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Creates a new office
     */
    @Transactional
    public Office create(Office office, String orgId) {
        // Organization should already be set on the office entity from the controller/mapper
        if (office.getOrganization() == null && orgId != null) {
            Organization org = authHelper.validateOrg(orgId);
            office.setOrganization(org);
        }
        
        // Validate name
        String name = office.getName() != null ? office.getName().trim() : "";
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        office.setName(name);
        
        // Normalize name and check for collision
        String normalizedName = validationHelper.normalizeName(name);
        Organization org = office.getOrganization();
        if (org == null) {
            throw new BadRequestException("Organization is required");
        }
        if (officeRepository.findByOrgIdAndNormalizedName(org.getId(), normalizedName)
            .filter(o -> o.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Office name already exists for this organization");
        }
        office.setNormalizedName(normalizedName);
        
        // Validate parent if provided
        if (office.getParent() != null && office.getParent().getId() != null) {
            Office parent = officeRepository.findById(office.getParent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent org location not found"));
            if (parent.getArchivedAt() != null) {
                throw new ResourceNotFoundException("Parent org location not found");
            }
            office.setParent(parent);
        }
        
        // Normalize tag keys
        if (office.getTagKeys() != null) {
            office.setTagKeys(validationHelper.normalizeKeys(office.getTagKeys()));
        }
        
        Office savedOffice = officeRepository.save(office);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", savedOffice.getName());
        metadata.put("orgLocationTypeKey", savedOffice.getOrgLocationTypeKey());
        metadata.put("parentOrgLocationId", savedOffice.getParent() != null ? savedOffice.getParent().getId() : null);
        
        auditLogService.log(
            "office.created",
            savedOffice.getOrganization() != null ? savedOffice.getOrganization().getId().toString() : null,
            null, // userId will be set when sessions are implemented
            "Office",
            savedOffice.getId().toString(),
            metadata
        );
        
        return savedOffice;
    }
    
    /**
     * Lists offices for an organization
     */
    @Transactional(readOnly = true)
    public List<Office> list(String orgId, boolean includeArchived) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        
        Long orgIdLong = Long.parseLong(orgId);
        
        if (includeArchived) {
            return officeRepository.findByOrgId(orgIdLong);
        } else {
            return officeRepository.findByOrgIdAndArchivedAtIsNull(orgIdLong);
        }
    }
    
    /**
     * Gets an office by ID
     */
    @Transactional(readOnly = true)
    public Office getById(Long id, String orgId, boolean includeArchived) {
        Office office = officeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Office not found"));
        
        if (orgId != null && (office.getOrganization() == null || 
            !office.getOrganization().getId().toString().equals(orgId))) {
            throw new ForbiddenException("Cannot access office outside your organization");
        }
        
        if (!includeArchived && office.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Office not found");
        }
        
        return office;
    }
    
    /**
     * Updates an office
     */
    @Transactional
    public Office update(Long id, Office officeUpdates, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        Office office = officeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Office not found"));
        
        if (office.getOrganization() != null) {
            authHelper.validateOrg(office.getOrganization().getId().toString());
        }
        authHelper.ensureNotOnLegalHold(office, "update");
        
        // Update name with normalization and collision check
        if (officeUpdates.getName() != null && !officeUpdates.getName().equals(office.getName())) {
            String normalizedName = validationHelper.normalizeName(officeUpdates.getName());
            if (officeRepository.findByOrgIdAndNormalizedName(office.getOrganization().getId(), normalizedName)
                .filter(o -> !o.getId().equals(id) && o.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Office name already exists for this organization");
            }
            office.setName(officeUpdates.getName());
            office.setNormalizedName(normalizedName);
        }
        
        // Update other fields
        if (officeUpdates.getDescription() != null) {
            office.setDescription(officeUpdates.getDescription());
        }
        if (officeUpdates.getTimezone() != null) {
            office.setTimezone(officeUpdates.getTimezone());
        }
        if (officeUpdates.getOrgLocationTypeKey() != null) {
            office.setOrgLocationTypeKey(officeUpdates.getOrgLocationTypeKey());
        }
        if (officeUpdates.getTagKeys() != null) {
            office.setTagKeys(validationHelper.normalizeKeys(officeUpdates.getTagKeys()));
        }
        if (officeUpdates.getAddress() != null) {
            office.setAddress(officeUpdates.getAddress());
        }
        if (officeUpdates.getSortOrder() != null) {
            office.setSortOrder(officeUpdates.getSortOrder());
        }
        
        // Update parent if provided
        if (officeUpdates.getParent() != null) {
            if (officeUpdates.getParent().getId() != null) {
                Office parent = officeRepository.findById(officeUpdates.getParent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent org location not found"));
                office.setParent(parent);
            } else {
                office.setParent(null);
            }
        }
        
        Office savedOffice = officeRepository.save(office);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", Map.of("updated", true));
        
        auditLogService.log(
            "office.updated",
            savedOffice.getOrganization().getId().toString(),
            null, // userId will be set when sessions are implemented
            "Office",
            savedOffice.getId().toString(),
            metadata
        );
        
        return savedOffice;
    }
    
    /**
     * Archives an office
     */
    @Transactional
    public Office archive(Long id, String orgId) {
        Office office = officeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Office not found"));
        
        if (orgId != null && office.getOrganization() != null && 
            !office.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access office outside your organization");
        }
        
        if (office.getOrganization() != null) {
            authHelper.validateOrg(office.getOrganization().getId().toString());
        }
        authHelper.ensureNotOnLegalHold(office, "archive");
        
        if (office.getArchivedAt() != null) {
            return office;
        }
        
        office.setArchivedAt(LocalDateTime.now());
        Office savedOffice = officeRepository.save(office);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedOffice.getArchivedAt());
        
        auditLogService.log(
            "office.archived",
            savedOffice.getOrganization().getId().toString(),
            null, // userId will be set when sessions are implemented
            "Office",
            savedOffice.getId().toString(),
            metadata
        );
        
        return savedOffice;
    }
    
    /**
     * Unarchives an office
     */
    @Transactional
    public Office unarchive(Long id, String orgId) {
        Office office = officeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Office not found"));
        
        if (orgId != null && office.getOrganization() != null && 
            !office.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access office outside your organization");
        }
        
        if (office.getOrganization() != null) {
            authHelper.validateOrg(office.getOrganization().getId().toString());
        }
        authHelper.ensureNotOnLegalHold(office, "unarchive");
        
        if (office.getArchivedAt() == null) {
            return office;
        }
        
        // Check for name collision when unarchiving
        String normalizedName = office.getNormalizedName() != null 
            ? office.getNormalizedName() 
            : validationHelper.normalizeName(office.getName());
        if (officeRepository.findByOrgIdAndNormalizedName(office.getOrganization().getId(), normalizedName)
            .filter(o -> !o.getId().equals(id) && o.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Office name already exists for this organization");
        }
        
        office.setArchivedAt(null);
        Office savedOffice = officeRepository.save(office);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedOffice.getArchivedAt());
        
        auditLogService.log(
            "office.unarchived",
            savedOffice.getOrganization().getId().toString(),
            null, // userId will be set when sessions are implemented
            "Office",
            savedOffice.getId().toString(),
            metadata
        );
        
        return savedOffice;
    }
}

