package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.ProjectResponseDto;
import com.mytegroup.api.entity.projects.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectResponseMapper {
    public ProjectResponseDto toDto(Project entity) {
        if (entity == null) {
            return null;
        }
        
        return ProjectResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId().toString() : null)
                .officeId(entity.getOffice() != null ? entity.getOffice().getId().toString() : null)
                .projectManager(entity.getProjectManager())
                .status(entity.getStatus())
                .isActive(entity.getIsActive())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

