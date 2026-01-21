package com.mytegroup.api.service.projects;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.organization.OfficeRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.service.common.ActorContext;
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
 * Service for project management.
 * Handles CRUD operations, project budgets, quantities, staffing, and cost code budgets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectsService {
    
    private final ProjectRepository projectRepository;
    private final OfficeRepository officeRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Creates a new project
     */
    @Transactional
    public Project create(Project project, ActorContext actor) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER);
        
        String orgId = project.getOrganization() != null 
            ? project.getOrganization().getId().toString() 
            : null;
        orgId = authHelper.resolveOrgId(orgId, actor);
        Organization org = authHelper.validateOrg(orgId);
        project.setOrganization(org);
        
        // Validate office if provided
        if (project.getOffice() != null && project.getOffice().getId() != null) {
            Office office = officeRepository.findById(project.getOffice().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Office not found or archived for this organization"));
            if (office.getArchivedAt() != null) {
                throw new ResourceNotFoundException("Office not found or archived for this organization");
            }
            project.setOffice(office);
        }
        
        // Validate name
        String name = project.getName() != null ? project.getName().trim() : "";
        if (name.isEmpty()) {
            throw new BadRequestException("Project name is required");
        }
        project.setName(name);
        
        // Check for name collision
        if (projectRepository.findByOrgIdAndName(org.getId(), name)
            .filter(p -> p.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Project name already exists for this organization");
        }
        
        // Check for project code collision
        if (project.getProjectCode() != null && !project.getProjectCode().trim().isEmpty()) {
            String projectCode = project.getProjectCode().trim();
            if (projectRepository.findByOrgIdAndProjectCode(org.getId(), projectCode)
                .filter(p -> p.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Project code already exists for this organization");
            }
            project.setProjectCode(projectCode);
        }
        
        Project savedProject = projectRepository.save(project);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", savedProject.getName());
        metadata.put("projectCode", savedProject.getProjectCode());
        metadata.put("status", savedProject.getStatus());
        metadata.put("officeId", savedProject.getOffice() != null ? savedProject.getOffice().getId() : null);
        
        auditLogService.log(
            "project.created",
            orgId,
            actor != null ? actor.getUserId() : null,
            "Project",
            savedProject.getId().toString(),
            metadata
        );
        
        return savedProject;
    }
    
    /**
     * Lists projects for an organization
     */
    @Transactional(readOnly = true)
    public List<Project> list(ActorContext actor, String orgId, boolean includeArchived) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER, Role.PM, Role.VIEWER);
        
        String resolvedOrgId = authHelper.resolveOrgId(orgId, actor);
        Long orgIdLong = Long.parseLong(resolvedOrgId);
        
        if (includeArchived && !authHelper.canViewArchived(actor)) {
            throw new ForbiddenException("Not allowed to include archived projects");
        }
        
        if (includeArchived) {
            return projectRepository.findByOrgId(orgIdLong);
        } else {
            return projectRepository.findByOrgIdAndArchivedAtIsNull(orgIdLong, 
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        }
    }
    
    /**
     * Gets a project by ID
     */
    @Transactional(readOnly = true)
    public Project getById(Long id, ActorContext actor, boolean includeArchived) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER, Role.PM, Role.VIEWER);
        
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (project.getOrganization() != null) {
            authHelper.ensureOrgScope(project.getOrganization().getId().toString(), actor);
        }
        
        if (project.getArchivedAt() != null && !includeArchived) {
            throw new ResourceNotFoundException("Project archived");
        }
        
        if (project.getArchivedAt() != null && includeArchived && !authHelper.canViewArchived(actor)) {
            throw new ForbiddenException("Not allowed to view archived projects");
        }
        
        return project;
    }
    
    /**
     * Updates a project
     */
    @Transactional
    public Project update(Long id, Project projectUpdates, ActorContext actor) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER);
        
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (project.getOrganization() != null) {
            authHelper.ensureOrgScope(project.getOrganization().getId().toString(), actor);
        }
        
        authHelper.validateOrg(project.getOrganization().getId().toString());
        
        if (project.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Project archived");
        }
        
        authHelper.ensureNotOnLegalHold(project, "update");
        
        // Update name if provided
        if (projectUpdates.getName() != null && !projectUpdates.getName().equals(project.getName())) {
            String newName = projectUpdates.getName().trim();
            if (projectRepository.findByOrgIdAndName(project.getOrganization().getId(), newName)
                .filter(p -> !p.getId().equals(id) && p.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Project name already exists for this organization");
            }
            project.setName(newName);
        }
        
        // Update other fields
        if (projectUpdates.getDescription() != null) {
            project.setDescription(projectUpdates.getDescription());
        }
        if (projectUpdates.getProjectCode() != null) {
            String projectCode = projectUpdates.getProjectCode().trim();
            if (!projectCode.isEmpty()) {
                if (projectRepository.findByOrgIdAndProjectCode(project.getOrganization().getId(), projectCode)
                    .filter(p -> !p.getId().equals(id) && p.getArchivedAt() == null)
                    .isPresent()) {
                    throw new ConflictException("Project code already exists for this organization");
                }
            }
            project.setProjectCode(projectCode.isEmpty() ? null : projectCode);
        }
        if (projectUpdates.getStatus() != null) {
            project.setStatus(projectUpdates.getStatus());
        }
        if (projectUpdates.getLocation() != null) {
            project.setLocation(projectUpdates.getLocation());
        }
        if (projectUpdates.getBudget() != null) {
            project.setBudget(projectUpdates.getBudget());
        }
        if (projectUpdates.getQuantities() != null) {
            project.setQuantities(projectUpdates.getQuantities());
        }
        if (projectUpdates.getCostCodeBudgets() != null) {
            project.setCostCodeBudgets(projectUpdates.getCostCodeBudgets());
        }
        if (projectUpdates.getBidDate() != null) {
            project.setBidDate(projectUpdates.getBidDate());
        }
        if (projectUpdates.getAwardDate() != null) {
            project.setAwardDate(projectUpdates.getAwardDate());
        }
        if (projectUpdates.getFabricationStartDate() != null) {
            project.setFabricationStartDate(projectUpdates.getFabricationStartDate());
        }
        if (projectUpdates.getFabricationEndDate() != null) {
            project.setFabricationEndDate(projectUpdates.getFabricationEndDate());
        }
        if (projectUpdates.getErectionStartDate() != null) {
            project.setErectionStartDate(projectUpdates.getErectionStartDate());
        }
        if (projectUpdates.getErectionEndDate() != null) {
            project.setErectionEndDate(projectUpdates.getErectionEndDate());
        }
        if (projectUpdates.getCompletionDate() != null) {
            project.setCompletionDate(projectUpdates.getCompletionDate());
        }
        
        // Update office if provided
        if (projectUpdates.getOffice() != null) {
            if (projectUpdates.getOffice().getId() != null) {
                Office office = officeRepository.findById(projectUpdates.getOffice().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Office not found or archived for this organization"));
                if (office.getArchivedAt() != null) {
                    throw new ResourceNotFoundException("Office not found or archived for this organization");
                }
                project.setOffice(office);
            } else {
                project.setOffice(null);
            }
        }
        
        Project savedProject = projectRepository.save(project);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", Map.of("updated", true));
        
        auditLogService.log(
            "project.updated",
            savedProject.getOrganization().getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Project",
            savedProject.getId().toString(),
            metadata
        );
        
        return savedProject;
    }
    
    /**
     * Archives a project
     */
    @Transactional
    public Project archive(Long id, ActorContext actor) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER);
        
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (project.getOrganization() != null) {
            authHelper.ensureOrgScope(project.getOrganization().getId().toString(), actor);
        }
        
        authHelper.validateOrg(project.getOrganization().getId().toString());
        authHelper.ensureNotOnLegalHold(project, "archive");
        
        if (project.getArchivedAt() != null) {
            return project;
        }
        
        project.setArchivedAt(LocalDateTime.now());
        Project savedProject = projectRepository.save(project);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedProject.getArchivedAt());
        
        auditLogService.log(
            "project.archived",
            savedProject.getOrganization().getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Project",
            savedProject.getId().toString(),
            metadata
        );
        
        return savedProject;
    }
    
    /**
     * Unarchives a project
     */
    @Transactional
    public Project unarchive(Long id, ActorContext actor) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER);
        
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (project.getOrganization() != null) {
            authHelper.ensureOrgScope(project.getOrganization().getId().toString(), actor);
        }
        
        authHelper.validateOrg(project.getOrganization().getId().toString());
        authHelper.ensureNotOnLegalHold(project, "unarchive");
        
        if (project.getArchivedAt() == null) {
            return project;
        }
        
        // Check for name collision when unarchiving
        if (projectRepository.findByOrgIdAndName(project.getOrganization().getId(), project.getName())
            .filter(p -> !p.getId().equals(id) && p.getArchivedAt() == null)
            .isPresent()) {
            throw new ConflictException("Project name already exists for this organization");
        }
        
        project.setArchivedAt(null);
        Project savedProject = projectRepository.save(project);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedProject.getArchivedAt());
        
        auditLogService.log(
            "project.unarchived",
            savedProject.getOrganization().getId().toString(),
            actor != null ? actor.getUserId() : null,
            "Project",
            savedProject.getId().toString(),
            metadata
        );
        
        return savedProject;
    }
}

