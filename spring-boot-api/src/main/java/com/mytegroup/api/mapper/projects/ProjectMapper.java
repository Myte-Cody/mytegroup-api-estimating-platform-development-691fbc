package com.mytegroup.api.mapper.projects;

import com.mytegroup.api.dto.projects.CreateProjectDto;
import com.mytegroup.api.dto.projects.UpdateProjectDto;
import com.mytegroup.api.dto.response.ProjectResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.projects.embeddable.ProjectBudget;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProjectMapper {

    /**
     * Maps CreateProjectDto to Project entity.
     */
    public Project toEntity(CreateProjectDto dto, Organization organization, Office office) {
        Project project = new Project();
        project.setName(dto.name());
        project.setOrganization(organization);
        project.setOffice(office);
        project.setDescription(dto.description());
        project.setProjectCode(dto.projectCode());
        project.setStatus(dto.status());
        project.setLocation(dto.location());
        project.setBidDate(dto.bidDate());
        project.setAwardDate(dto.awardDate());
        project.setFabricationStartDate(dto.fabricationStartDate());
        project.setFabricationEndDate(dto.fabricationEndDate());
        project.setErectionStartDate(dto.erectionStartDate());
        project.setErectionEndDate(dto.erectionEndDate());
        project.setCompletionDate(dto.completionDate());
        
        // Map budget
        if (dto.budget() != null) {
            ProjectBudget budget = new ProjectBudget();
            Map<String, Object> budgetMap = dto.budget();
            if (budgetMap.containsKey("hours")) {
                budget.setHours(((Number) budgetMap.get("hours")).doubleValue());
            }
            if (budgetMap.containsKey("labourRate")) {
                budget.setLabourRate(((Number) budgetMap.get("labourRate")).doubleValue());
            }
            if (budgetMap.containsKey("currency")) {
                budget.setCurrency((String) budgetMap.get("currency"));
            }
            if (budgetMap.containsKey("amount")) {
                budget.setAmount(((Number) budgetMap.get("amount")).doubleValue());
            }
            project.setBudget(budget);
        }
        
        // TODO: Map quantities, staffing, costCodeBudgets if needed
        
        return project;
    }

    /**
     * Updates existing Project entity with UpdateProjectDto values.
     */
    public void updateEntity(Project project, UpdateProjectDto dto, Office office) {
        if (dto.name() != null) {
            project.setName(dto.name());
        }
        if (office != null) {
            project.setOffice(office);
        }
        if (dto.description() != null) {
            project.setDescription(dto.description());
        }
        if (dto.projectCode() != null) {
            project.setProjectCode(dto.projectCode());
        }
        if (dto.status() != null) {
            project.setStatus(dto.status());
        }
        if (dto.location() != null) {
            project.setLocation(dto.location());
        }
        if (dto.bidDate() != null) {
            project.setBidDate(dto.bidDate());
        }
        if (dto.awardDate() != null) {
            project.setAwardDate(dto.awardDate());
        }
        if (dto.fabricationStartDate() != null) {
            project.setFabricationStartDate(dto.fabricationStartDate());
        }
        if (dto.fabricationEndDate() != null) {
            project.setFabricationEndDate(dto.fabricationEndDate());
        }
        if (dto.erectionStartDate() != null) {
            project.setErectionStartDate(dto.erectionStartDate());
        }
        if (dto.erectionEndDate() != null) {
            project.setErectionEndDate(dto.erectionEndDate());
        }
        if (dto.completionDate() != null) {
            project.setCompletionDate(dto.completionDate());
        }
        
        // Update budget if provided
        if (dto.budget() != null) {
            ProjectBudget budget = project.getBudget();
            if (budget == null) {
                budget = new ProjectBudget();
            }
            Map<String, Object> budgetMap = dto.budget();
            if (budgetMap.containsKey("hours")) {
                budget.setHours(((Number) budgetMap.get("hours")).doubleValue());
            }
            if (budgetMap.containsKey("labourRate")) {
                budget.setLabourRate(((Number) budgetMap.get("labourRate")).doubleValue());
            }
            if (budgetMap.containsKey("currency")) {
                budget.setCurrency((String) budgetMap.get("currency"));
            }
            if (budgetMap.containsKey("amount")) {
                budget.setAmount(((Number) budgetMap.get("amount")).doubleValue());
            }
            project.setBudget(budget);
        }
        
        // TODO: Update quantities, staffing, costCodeBudgets if needed
    }

    /**
     * Maps Project entity to ProjectResponseDto.
     */
    public ProjectResponseDto toDto(Project entity) {
        if (entity == null) {
            return null;
        }
        
        return ProjectResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

