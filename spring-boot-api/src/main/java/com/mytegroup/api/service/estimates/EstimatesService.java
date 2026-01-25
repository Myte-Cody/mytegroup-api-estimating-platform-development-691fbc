package com.mytegroup.api.service.estimates;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.repository.projects.EstimateRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    
    /**
     * Creates a new estimate
     */
    @Transactional
    public Estimate create(Estimate estimate, Long projectId, String orgId) {
        if (projectId == null) {
            throw new BadRequestException("Project ID is required");
        }
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (project.getOrganization() == null || 
            !project.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Project not found");
        }
        
        estimate.setProject(project);
        
        estimate.setOrganization(project.getOrganization());
        
        // Validate name
        if (estimate.getName() == null || estimate.getName().trim().isEmpty()) {
            throw new BadRequestException("Estimate name is required");
        }
        
        Long orgIdLong = Long.parseLong(orgId);
        if (estimateRepository.findByOrganization_IdAndProjectIdAndName(orgIdLong, project.getId(), estimate.getName())
            .filter(e -> e.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Estimate name already exists for this project");
        }
        
        // Set createdByUser from security context
        if (estimate.getCreatedByUser() == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                String username = authentication.getName();
                userRepository.findByUsername(username)
                    .filter(u -> u.getOrganization() != null && u.getOrganization().getId().toString().equals(orgId))
                    .ifPresent(estimate::setCreatedByUser);
            }
        }
        
        Estimate savedEstimate = estimateRepository.save(estimate);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", savedEstimate.getName());
        metadata.put("projectId", project.getId().toString());
        
        auditLogService.log(
            "estimate.created",
            orgId,
            null,
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
    public List<Estimate> listByProject(Long projectId, String orgId, Boolean includeArchived) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Long orgIdLong = Long.parseLong(orgId);
        
        if (includeArchived == null) {
            includeArchived = false;
        }
        
        if (Boolean.TRUE.equals(includeArchived)) {
            // Filter by orgId manually since repository doesn't have this method
            return estimateRepository.findByProjectId(projectId).stream()
                .filter(e -> e.getOrganization() != null && e.getOrganization().getId().equals(orgIdLong))
                .toList();
        } else {
            return estimateRepository.findByOrganization_IdAndProjectIdAndArchivedAtIsNull(orgIdLong, projectId);
        }
    }
    
    /**
     * Gets an estimate by ID
     */
    @Transactional(readOnly = true)
    public Estimate getById(Long id, Long projectId, String orgId, boolean includeArchived) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Estimate estimate = estimateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));
        
        if (estimate.getOrganization() == null || 
            !estimate.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access estimate outside your organization");
        }
        
        if (estimate.getProject() == null || !estimate.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Estimate not found for this project");
        }
        
        if (!includeArchived && estimate.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Estimate not found");
        }
        
        return estimate;
    }
    
    /**
     * Updates an estimate
     */
    @Transactional
    public Estimate update(Long id, Long projectId, Estimate estimateUpdates, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Estimate estimate = estimateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));
        
        if (estimate.getOrganization() == null || 
            !estimate.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot update estimate outside your organization");
        }
        
        if (estimate.getProject() == null || !estimate.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Estimate not found for this project");
        }
        
        authHelper.ensureNotOnLegalHold(estimate, "update");
        
        // Update name with collision check
        if (estimateUpdates.getName() != null && !estimateUpdates.getName().equals(estimate.getName())) {
            Long orgIdLong = estimate.getOrganization().getId();
            Long existingProjectId = estimate.getProject().getId();
            if (estimateRepository.findByOrganization_IdAndProjectIdAndName(orgIdLong, existingProjectId, estimateUpdates.getName())
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
            null,
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
    public Estimate archive(Long id, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Estimate estimate = estimateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));
        
        if (estimate.getOrganization() == null || 
            !estimate.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot archive estimate outside your organization");
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
            null,
            "Estimate",
            savedEstimate.getId().toString(),
            metadata
        );
        
        return savedEstimate;
    }
}

