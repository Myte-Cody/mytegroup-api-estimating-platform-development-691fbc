package com.mytegroup.api.mapper.crewswaps;

import com.mytegroup.api.dto.crewswaps.CreateCrewSwapDto;
import com.mytegroup.api.dto.response.CrewSwapResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.crew.CrewSwap;
import com.mytegroup.api.entity.enums.crew.CrewSwapStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import org.springframework.stereotype.Component;

@Component
public class CrewSwapMapper {

    public CrewSwap toEntity(CreateCrewSwapDto dto, Organization org, Project project, Person person) {
        CrewSwap swap = new CrewSwap();
        swap.setOrganization(org);
        swap.setProject(project);
        swap.setPerson(person);
        swap.setFromCrewId(dto.fromCrewId());
        swap.setToCrewId(dto.toCrewId());
        swap.setRequestedBy(dto.requestedBy());
        swap.setRequestedAt(dto.requestedAt());
        swap.setStatus(CrewSwapStatus.REQUESTED);
        return swap;
    }

    public CrewSwapResponseDto toDto(CrewSwap entity) {
        if (entity == null) {
            return null;
        }
        return CrewSwapResponseDto.builder()
            .id(entity.getId())
            .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
            .projectId(entity.getProject() != null ? entity.getProject().getId().toString() : null)
            .personId(entity.getPerson() != null ? entity.getPerson().getId().toString() : null)
            .fromCrewId(entity.getFromCrewId())
            .toCrewId(entity.getToCrewId())
            .status(entity.getStatus() != null ? entity.getStatus().getValue() : null)
            .requestedBy(entity.getRequestedBy())
            .requestedAt(entity.getRequestedAt())
            .approvedBy(entity.getApprovedBy())
            .approvedAt(entity.getApprovedAt())
            .rejectedBy(entity.getRejectedBy())
            .rejectedAt(entity.getRejectedAt())
            .rejectionReason(entity.getRejectionReason())
            .completedBy(entity.getCompletedBy())
            .completedAt(entity.getCompletedAt())
            .archivedAt(entity.getArchivedAt())
            .piiStripped(entity.getPiiStripped())
            .legalHold(entity.getLegalHold())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
