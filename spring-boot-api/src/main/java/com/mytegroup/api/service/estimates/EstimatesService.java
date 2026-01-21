package com.mytegroup.api.service.estimates;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.projects.EstimateRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for estimate management.
 * Handles CRUD operations for project estimates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstimatesService {
    
    private final EstimateRepository estimateRepository;
    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    
    /**
     * Creates a new estimate
     */
    @Transactional
    public Estimate create(Estimate estimate, ActorContext actor) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER, Role.ESTIMATOR);
        
        if (estimate.getProject() == null || estimate.getProject().getId() == null) {
            throw new BadRequestException("Project is required");
        }
        
        Project project = projectRepository.findById(estimate.getProject().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (project.getOrganization() != null) {
            authHelper.ensureOrgScope(project.getOrganization().getId().toString(), actor);
        }
        
        estimate.setProject(project);
        estimate.setOrganization(project.getOrganization());
        
        // Validate name
        if (estimate.getName() == null || estimate.getName().trim().isEmpty()) {
            throw new BadRequestException("Estimate name is required");
        }
        
        // Check for name collision within project
        String orgId = project.getOrganization().getId().toString();
        Long orgIdLong = project.getOrganization().getId();
        if (estimateRepository.findByOrgIdAndProjectIdAndName(orgIdLong, project.getId(), estimate.getName())
            .filter(e -> e.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Estimate name already exists for this project");
        }
        
        // TODO: Set createdByUser from actor
        
        Estimate savedEstimate = estimateRepository.save(estimate);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", savedEstimate.getName());
        metadata.put("projectId", project.getId().toString());
        
        auditLogService.log(
            "estimate.created",
            orgId,
            actor != null ? actor.getUserId() : null,
            "Estimate",
            savedEstimate.getId().toString(),
            metadata
        );
        
        return savedEstimate;
    }
    
    /**
     * Lists estimates for a project
     */
    @Transactional(readOnly = true)
    public List<Estimate> list(ActorContext actor, String orgId, Long projectId, boolean includeArchived) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER, Role.ESTIMATOR, Role.VIEWER);
        
        String resolvedOrgId = authHelper.resolveOrgId(orgId, actor);
        authHelper.ensureOrgScope(resolvedOrgId, actor);
        
        Long orgIdLong = Long.parseLong(resolvedOrgId);
        
        if (includeArchived && !authHelper.canViewArchived(actor)) {
            throw new ForbiddenException("Not allowed to include archived estimates");
        }
        
        if (includeArchived) {
            return estimateRepository.findByOrgIdAndProjectId(orgIdLong, projectId);
        } else {
            return estimateRepository.findByOrgIdAndProjectIdAndArchivedAtIsNull(orgIdLong, projectId);
        }
    }
    
    /**
     * Gets an estimate by ID
     */
    @Transactional(readOnly = true)
    public Estimate getById(Long id, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER, Role.ESTIMATOR, Role.VIEWER);
        
        String resolvedOrgId = authHelper.resolveOrgId(orgId, actor);
        authHelper.ensureOrgScope(resolvedOrgId, actor);
        
        Estimate estimate = estimateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));
        
        if (estimate.getOrganization() == null || 
            !estimate.getOrganization().getId().toString().equals(resolvedOrgId)) {
            throw new ForbiddenException("Cannot access estimate outside your organization");
        }
        
        return estimate;
    }
    
    /**
     * Updates an estimate
     */
    @Transactional
    public Estimate update(Long id, Estimate estimateUpdates, ActorContext actor) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER, Role.ESTIMATOR);
        
        Estimate estimate = estimateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));
        
        if (estimate.getOrganization() != null) {
            authHelper.ensureOrgScope(estimate.getOrganization().getId().toString(), actor);
        }
        
        authHelper.ensureNotOnLegalHold(estimate, "update");
        
        // Update name with collision check
        if (estimateUpdates.getName() != null && !estimateUpdates.getName().equals(estimate.getName())) {
            Long orgIdLong = estimate.getOrganization().getId();
            Long projectId = estimate.getProject().getId();
            if (estimateRepository.findByOrgIdAndProjectIdAndName(orgIdLong, projectId, estimateUpdates.getName())
                .filter(e -> !e.getId().equals(id) && e.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Estimate name already exists for this project");
            }
            estimate.setName(estimateUpdates.getName());
        }
        
        // Update other fields
        if (estimateUpdates.getDescription() != null) {
            estimate.setDescription(estimateUpdates.getDescription());
        }
        if (estimateUpdates.getStatus() != null) {
            estimate.setStatus(estimateUpdates.getStatus());
        }
        if (estimateUpdates.getTotalAmount() != null) {
            estimate.setTotalAmount(estimateUpdates.getTotalAmount());
        }
        if (estimateUpdates.getLineItems() != null) {
            estimate.setLineItems(estimateUpdates.getLineItems());
        }
        if (estimateUpdates.getNotes() != null) {
            estimate.setNotes(estimateUpdates.getNotes());
        }
        
        Estimate savedEstimate = estimateRepository.save(estimate);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", Map.of("updated", true));
        
        auditLogService.log(
            "estimate.updated",
            savedEstimate.getOrganization().getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Estimate",
            savedEstimate.getId().toString(),
            metadata
        );
        
        return savedEstimate;
    }
    
    /**
     * Archives an estimate
     */
    @Transactional
    public Estimate archive(Long id, ActorContext actor) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER);
        
        Estimate estimate = estimateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));
        
        if (estimate.getOrganization() != null) {
            authHelper.ensureOrgScope(estimate.getOrganization().getId().toString(), actor);
        }
        
        authHelper.ensureNotOnLegalHold(estimate, "archive");
        
        if (estimate.getArchivedAt() != null) {
            return estimate;
        }
        
        estimate.setArchivedAt(LocalDateTime.now());
        Estimate savedEstimate = estimateRepository.save(estimate);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedEstimate.getArchivedAt());
        
        auditLogService.log(
            "estimate.archived",
            savedEstimate.getOrganization().getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Estimate",
            savedEstimate.getId().toString(),
            metadata
        );
        
        return savedEstimate;
    }
}

