package com.mytegroup.api.mapper.crewassignments;

import com.mytegroup.api.dto.crewassignments.CreateCrewAssignmentDto;
import com.mytegroup.api.dto.crewassignments.UpdateCrewAssignmentDto;
import com.mytegroup.api.dto.response.CrewAssignmentResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.crew.CrewAssignmentStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.crew.CrewAssignment;
import org.springframework.stereotype.Component;

@Component
public class CrewAssignmentMapper {

    public CrewAssignment toEntity(CreateCrewAssignmentDto dto, Organization org, Project project, Person person) {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setOrganization(org);
        assignment.setProject(project);
        assignment.setPerson(person);
        assignment.setCrewId(dto.crewId());
        assignment.setRoleKey(dto.roleKey());
        assignment.setStartDate(dto.startDate());
        assignment.setEndDate(dto.endDate());
        assignment.setCreatedBy(dto.createdBy());
        return assignment;
    }

    public void updateEntity(CrewAssignment assignment, UpdateCrewAssignmentDto dto) {
        if (dto.crewId() != null) {
            assignment.setCrewId(dto.crewId());
        }
        if (dto.roleKey() != null) {
            assignment.setRoleKey(dto.roleKey());
        }
        if (dto.startDate() != null) {
            assignment.setStartDate(dto.startDate());
        }
        if (dto.endDate() != null) {
            assignment.setEndDate(dto.endDate());
        }
        if (dto.status() != null) {
            assignment.setStatus(parseStatus(dto.status()));
        }
    }

    public CrewAssignmentResponseDto toDto(CrewAssignment entity) {
        if (entity == null) {
            return null;
        }
        return CrewAssignmentResponseDto.builder()
            .id(entity.getId())
            .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
            .projectId(entity.getProject() != null ? entity.getProject().getId().toString() : null)
            .personId(entity.getPerson() != null ? entity.getPerson().getId().toString() : null)
            .crewId(entity.getCrewId())
            .roleKey(entity.getRoleKey())
            .startDate(entity.getStartDate())
            .endDate(entity.getEndDate())
            .status(entity.getStatus() != null ? entity.getStatus().getValue() : null)
            .createdBy(entity.getCreatedBy())
            .archivedAt(entity.getArchivedAt())
            .piiStripped(entity.getPiiStripped())
            .legalHold(entity.getLegalHold())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private CrewAssignmentStatus parseStatus(String status) {
        try {
            return CrewAssignmentStatus.fromValue(status);
        } catch (IllegalArgumentException ex) {
            return CrewAssignmentStatus.ACTIVE;
        }
    }
}
