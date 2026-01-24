package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.EstimateResponseDto;
import com.mytegroup.api.entity.estimates.Estimate;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class EstimateResponseMapper {
    public EstimateResponseDto toDto(Estimate entity) {
        if (entity == null) {
            return null;
        }
        
        return EstimateResponseDto.builder()
                .id(entity.getId())
                .projectId(entity.getProject() != null ? entity.getProject().getId() : null)
                .estimateName(entity.getEstimateName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .lineItems(Collections.emptyList()) // TODO: Map line items if available
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

