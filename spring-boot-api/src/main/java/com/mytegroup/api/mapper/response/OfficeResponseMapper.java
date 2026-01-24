package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.OfficeResponseDto;
import com.mytegroup.api.entity.offices.Office;
import org.springframework.stereotype.Component;

@Component
public class OfficeResponseMapper {
    public OfficeResponseDto toDto(Office entity) {
        if (entity == null) {
            return null;
        }
        
        return OfficeResponseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .address(entity.getAddress())
                .city(entity.getCity())
                .region(entity.getRegion())
                .postal(entity.getPostal())
                .country(entity.getCountry())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .isActive(entity.getIsActive())
                .archivedAt(entity.getArchivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

