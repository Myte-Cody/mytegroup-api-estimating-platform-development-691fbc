package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.CompanyLocationResponseDto;
import com.mytegroup.api.entity.companies.CompanyLocation;
import org.springframework.stereotype.Component;

@Component
public class CompanyLocationResponseMapper {
    public CompanyLocationResponseDto toDto(CompanyLocation entity) {
        if (entity == null) {
            return null;
        }
        
        return CompanyLocationResponseDto.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .address(entity.getAddress())
                .city(entity.getCity())
                .region(entity.getRegion())
                .postal(entity.getPostal())
                .country(entity.getCountry())
                .archivedAt(entity.getArchivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

