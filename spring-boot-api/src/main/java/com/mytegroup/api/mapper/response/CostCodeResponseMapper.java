package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.CostCodeResponseDto;
import com.mytegroup.api.entity.costcodes.CostCode;
import org.springframework.stereotype.Component;

@Component
public class CostCodeResponseMapper {
    public CostCodeResponseDto toDto(CostCode entity) {
        if (entity == null) {
            return null;
        }
        
        return CostCodeResponseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

