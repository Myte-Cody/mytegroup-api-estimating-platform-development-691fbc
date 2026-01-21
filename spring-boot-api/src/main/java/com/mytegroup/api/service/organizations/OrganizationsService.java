package com.mytegroup.api.service.organizations;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import com.mytegroup.api.entity.enums.organization.DataResidency;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.OrganizationRepository;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for organization management.
 * Handles CRUD operations, datastore management, legal hold, and PII stripping.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationsService {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    
    /**
     * Finds an organization by domain.
     * Returns null if organization not found (for existence checks).
     * @throws BadRequestException if domain is invalid
     */
    @Transactional(readOnly = true)
    public Organization findByDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            throw new BadRequestException("Domain is required");
        }
        String normalized = domain.toLowerCase().trim();
        return organizationRepository.findByPrimaryDomain(normalized)
            .filter(org -> org.getArchivedAt() == null)
            .orElse(null);
    }
    
    /**
     * Creates a new organization
     */
    @Transactional
    public Organization create(Organization organization, ActorContext actor) {
        // Check for name collision
        if (organizationRepository.findByName(organization.getName()).isPresent()) {
            throw new ConflictException("Organization name already in use");
        }
        
        // Check for domain collision
        if (organization.getPrimaryDomain() != null) {
            String normalizedDomain = organization.getPrimaryDomain().toLowerCase().trim();
            if (organizationRepository.findByPrimaryDomain(normalizedDomain).isPresent()) {
                throw new ConflictException("Organization domain already registered");
            }
            organization.setPrimaryDomain(normalizedDomain);
        }
        
        // Validate datastore configuration
        DatastoreType datastoreType = organization.getDatastoreType();
        if (datastoreType == null) {
            datastoreType = organization.getUseDedicatedDb() ? DatastoreType.DEDICATED : DatastoreType.SHARED;
            organization.setDatastoreType(datastoreType);
        }
        
        if (datastoreType == DatastoreType.DEDICATED && 
            (organization.getDatabaseUri() == null || organization.getDatabaseUri().trim().isEmpty())) {
            throw new BadRequestException("Dedicated datastore requires a connection URI");
        }
        
        // Set data residency
        if (organization.getDataResidency() == null) {
            organization.setDataResidency(
                datastoreType == DatastoreType.DEDICATED ? DataResidency.DEDICATED : DataResidency.SHARED
            );
        }
        
        // Set defaults
        if (organization.getMetadata() == null) {
            organization.setMetadata(new HashMap<>());
        }
        if (organization.getPiiStripped() == null) {
            organization.setPiiStripped(false);
        }
        if (organization.getLegalHold() == null) {
            organization.setLegalHold(false);
        }
        if (organization.getUseDedicatedDb() == null) {
            organization.setUseDedicatedDb(datastoreType == DatastoreType.DEDICATED);
        }
        
        Organization savedOrg = organizationRepository.save(organization);
        
        // Audit log
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("datastore", sanitizeDatastoreSnapshot(savedOrg));
        auditLogService.log(
            "organization.created",
            savedOrg.getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Organization",
            savedOrg.getId().toString(),
            metadata
        );
        
        // TODO: Ensure org seats - requires SeatsService
        // seatsService.ensureOrgSeats(savedOrg.getId(), defaultSeatsPerOrg);
        
        return savedOrg;
    }
    
    /**
     * Sets the owner of an organization
     */
    @Transactional
    public Organization setOwner(Long orgId, Long userId, ActorContext actor) {
        Organization org = getOrgOrThrow(orgId);
        authHelper.ensureNotOnLegalHold(org, "set owner");
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        org.setOwnerUser(user);
        if (org.getCreatedByUser() == null) {
            org.setCreatedByUser(user);
        }
        
        Organization savedOrg = organizationRepository.save(org);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ownerUserId", userId.toString());
        auditLogService.log(
            "organization.owner_assigned",
            savedOrg.getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Organization",
            savedOrg.getId().toString(),
            metadata
        );
        
        return savedOrg;
    }
    
    /**
     * Updates an organization
     */
    @Transactional
    public Organization update(Long id, Organization organizationUpdates, ActorContext actor) {
        Organization org = getOrgOrThrow(id);
        authHelper.ensureNotOnLegalHold(org, "update");
        
        // Check for name collision
        if (organizationUpdates.getName() != null && !organizationUpdates.getName().equals(org.getName())) {
            if (organizationRepository.findByName(organizationUpdates.getName()).isPresent()) {
                throw new ConflictException("Organization name already in use");
            }
            org.setName(organizationUpdates.getName());
        }
        
        // Update metadata if provided
        if (organizationUpdates.getMetadata() != null) {
            org.setMetadata(organizationUpdates.getMetadata());
        }
        
        Organization savedOrg = organizationRepository.save(org);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("before", Map.of("name", org.getName()));
        metadata.put("after", Map.of("name", savedOrg.getName()));
        auditLogService.log(
            "organization.updated",
            savedOrg.getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Organization",
            savedOrg.getId().toString(),
            metadata
        );
        
        return savedOrg;
    }
    
    /**
     * Updates organization datastore configuration
     */
    @Transactional
    public Organization updateDatastore(Long id, Organization datastoreUpdates, ActorContext actor) {
        Organization org = getOrgOrThrow(id);
        authHelper.ensureNotOnLegalHold(org, "update datastore");
        
        DatastoreType currentType = currentDatastoreType(org);
        DatastoreType targetType = datastoreUpdates.getDatastoreType();
        if (targetType == null) {
            targetType = Boolean.TRUE.equals(datastoreUpdates.getUseDedicatedDb()) 
                ? DatastoreType.DEDICATED 
                : currentType;
        }
        
        String targetUri = datastoreUpdates.getDatabaseUri() != null 
            ? datastoreUpdates.getDatabaseUri() 
            : org.getDatabaseUri();
        String targetDbName = datastoreUpdates.getDatabaseName() != null 
            ? datastoreUpdates.getDatabaseName() 
            : org.getDatabaseName();
        DataResidency targetResidency = datastoreUpdates.getDataResidency() != null
            ? datastoreUpdates.getDataResidency()
            : (targetType == DatastoreType.DEDICATED ? DataResidency.DEDICATED : DataResidency.SHARED);
        
        if (targetType == DatastoreType.DEDICATED) {
            if (targetUri == null || targetUri.trim().isEmpty()) {
                throw new BadRequestException("Dedicated datastore requires a connection URI");
            }
            // TODO: Test connection - requires TenantConnectionService
            // tenants.testConnection(targetUri, targetDbName);
        }
        
        Map<String, Object> before = new HashMap<>();
        before.put("useDedicatedDb", org.getUseDedicatedDb());
        before.put("datastoreType", currentType.getValue());
        before.put("databaseUri", org.getDatabaseUri());
        before.put("databaseName", org.getDatabaseName());
        before.put("dataResidency", org.getDataResidency().getValue());
        
        org.setUseDedicatedDb(targetType == DatastoreType.DEDICATED);
        org.setDatastoreType(targetType);
        org.setDatabaseUri(targetType == DatastoreType.DEDICATED ? targetUri : null);
        org.setDatabaseName(targetType == DatastoreType.DEDICATED ? targetDbName : null);
        org.setDataResidency(targetResidency);
        
        // Add to datastore history
        if (org.getDatastoreHistory() == null) {
            org.setDatastoreHistory(new java.util.ArrayList<>());
        }
        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("fromType", before.get("datastoreType"));
        historyEntry.put("toType", targetType.getValue());
        historyEntry.put("fromUri", before.get("databaseUri"));
        historyEntry.put("toUri", org.getDatabaseUri());
        historyEntry.put("actorId", actor != null ? actor.getUserId() : null);
        historyEntry.put("switchedAt", LocalDateTime.now());
        org.getDatastoreHistory().add(historyEntry);
        
        Organization savedOrg = organizationRepository.save(org);
        
        // TODO: Reset connection for org - requires TenantConnectionService
        // tenants.resetConnectionForOrg(id);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("before", sanitizeDatastoreSnapshot(org));
        metadata.put("after", sanitizeDatastoreSnapshot(savedOrg));
        auditLogService.log(
            "organization.datastore_switched",
            savedOrg.getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Organization",
            savedOrg.getId().toString(),
            metadata
        );
        
        return savedOrg;
    }
    
    /**
     * Sets legal hold status
     */
    @Transactional
    public Organization setLegalHold(Long id, Boolean legalHold, ActorContext actor) {
        Organization org = getOrgOrThrow(id);
        if (Boolean.TRUE.equals(org.getLegalHold()) == Boolean.TRUE.equals(legalHold)) {
            return org;
        }
        
        org.setLegalHold(legalHold);
        Organization savedOrg = organizationRepository.save(org);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("legalHold", savedOrg.getLegalHold());
        auditLogService.log(
            "organization.legal_hold_toggled",
            savedOrg.getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Organization",
            savedOrg.getId().toString(),
            metadata
        );
        
        return savedOrg;
    }
    
    /**
     * Sets PII stripped status
     */
    @Transactional
    public Organization setPiiStripped(Long id, Boolean piiStripped, ActorContext actor) {
        Organization org = getOrgOrThrow(id);
        if (Boolean.TRUE.equals(org.getPiiStripped()) == Boolean.TRUE.equals(piiStripped)) {
            return org;
        }
        
        org.setPiiStripped(piiStripped);
        Organization savedOrg = organizationRepository.save(org);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("piiStripped", savedOrg.getPiiStripped());
        auditLogService.log(
            "organization.pii_stripped_toggled",
            savedOrg.getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Organization",
            savedOrg.getId().toString(),
            metadata
        );
        
        return savedOrg;
    }
    
    /**
     * Finds an organization by ID
     */
    @Transactional(readOnly = true)
    public Organization findById(Long id) {
        return getOrgOrThrow(id);
    }
    
    /**
     * Finds all active organizations
     */
    @Transactional(readOnly = true)
    public List<Organization> findAll() {
        return organizationRepository.findByArchivedAtIsNull();
    }
    
    /**
     * Lists organizations with filtering and pagination
     */
    @Transactional(readOnly = true)
    public Page<Organization> list(String search, Boolean includeArchived, DatastoreType datastoreType, 
                                    Boolean legalHold, Boolean piiStripped, int page, int limit) {
        Specification<Organization> spec = Specification.where(null);
        
        if (!Boolean.TRUE.equals(includeArchived)) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("archivedAt")));
        }
        
        if (datastoreType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("datastoreType"), datastoreType));
        }
        
        if (legalHold != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("legalHold"), legalHold));
        }
        
        if (piiStripped != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("piiStripped"), piiStripped));
        }
        
        if (search != null && !search.trim().isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("name")), searchPattern),
                    cb.like(cb.lower(root.get("primaryDomain")), searchPattern)
                )
            );
        }
        
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return organizationRepository.findAll(spec, pageable);
    }
    
    /**
     * Checks if any organization exists
     */
    @Transactional(readOnly = true)
    public boolean hasAnyOrganization() {
        return organizationRepository.existsByArchivedAtIsNull();
    }
    
    /**
     * Archives an organization
     */
    @Transactional
    public Organization archive(Long id, ActorContext actor) {
        Organization org = getOrgOrThrow(id);
        if (Boolean.TRUE.equals(org.getLegalHold())) {
            throw new ForbiddenException("Organization is under legal hold");
        }
        if (org.getArchivedAt() != null) {
            return org;
        }
        
        org.setArchivedAt(LocalDateTime.now());
        Organization savedOrg = organizationRepository.save(org);
        
        auditLogService.log(
            "organization.archived",
            savedOrg.getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Organization",
            savedOrg.getId().toString(),
            null
        );
        
        return savedOrg;
    }
    
    /**
     * Unarchives an organization
     */
    @Transactional
    public Organization unarchive(Long id, ActorContext actor) {
        Organization org = getOrgOrThrow(id);
        if (org.getArchivedAt() == null) {
            return org;
        }
        
        org.setArchivedAt(null);
        Organization savedOrg = organizationRepository.save(org);
        
        auditLogService.log(
            "organization.unarchived",
            savedOrg.getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Organization",
            savedOrg.getId().toString(),
            null
        );
        
        return savedOrg;
    }
    
    // Helper methods
    
    private Organization getOrgOrThrow(Long id) {
        return organizationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
    
    private DatastoreType currentDatastoreType(Organization org) {
        return Boolean.TRUE.equals(org.getUseDedicatedDb()) || org.getDatastoreType() == DatastoreType.DEDICATED
            ? DatastoreType.DEDICATED
            : DatastoreType.SHARED;
    }
    
    private Map<String, Object> sanitizeDatastoreSnapshot(Organization org) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("useDedicatedDb", org.getUseDedicatedDb());
        snapshot.put("datastoreType", currentDatastoreType(org).getValue());
        snapshot.put("databaseName", org.getDatabaseName());
        snapshot.put("dataResidency", org.getDataResidency() != null ? org.getDataResidency().getValue() : null);
        return snapshot;
    }
}

