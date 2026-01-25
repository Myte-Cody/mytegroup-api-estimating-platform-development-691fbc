package com.mytegroup.api.mapper.costcodes;

import com.mytegroup.api.dto.costcodes.CreateCostCodeDto;
import com.mytegroup.api.dto.costcodes.CostCodeInputDto;
import com.mytegroup.api.dto.costcodes.UpdateCostCodeDto;
import com.mytegroup.api.dto.response.CostCodeResponseDto;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.entity.core.Organization;
import org.springframework.stereotype.Component;

@Component
public class CostCodeMapper {

    public CostCode toEntity(CreateCostCodeDto dto, Organization organization) {
        CostCode costCode = new CostCode();
        costCode.setOrganization(organization);
        costCode.setCategory(dto.category());
        costCode.setCode(dto.code());
        costCode.setDescription(dto.description());
        costCode.setActive(true);
        return costCode;
    }

    public CostCode toEntity(CostCodeInputDto dto, Organization organization) {
        CostCode costCode = new CostCode();
        costCode.setOrganization(organization);
        costCode.setCategory(dto.category());
        costCode.setCode(dto.code());
        costCode.setDescription(dto.description());
        costCode.setActive(true);
        return costCode;
    }

    public void updateEntity(CostCode costCode, UpdateCostCodeDto dto) {
        if (dto.category() != null) {
            costCode.setCategory(dto.category());
        }
        if (dto.code() != null) {
            costCode.setCode(dto.code());
        }
        if (dto.description() != null) {
            costCode.setDescription(dto.description());
        }
    }

    /**
     * Maps CostCode entity to CostCodeResponseDto.
     */
    public CostCodeResponseDto toDto(CostCode entity) {
        if (entity == null) {
            return null;
        }
        
        return CostCodeResponseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getCategory())
                .description(entity.getDescription())
                .active(entity.getActive())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

