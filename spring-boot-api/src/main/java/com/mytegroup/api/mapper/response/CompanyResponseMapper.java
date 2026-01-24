package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.CompanyResponseDto;
import com.mytegroup.api.entity.companies.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyResponseMapper {
    public CompanyResponseDto toDto(Company entity) {
        if (entity == null) {
            return null;
        }
        
        return CompanyResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .normalizedName(entity.getNormalizedName())
                .externalId(entity.getExternalId())
                .website(entity.getWebsite())
                .mainEmail(entity.getMainEmail())
                .mainPhone(entity.getMainPhone())
                .companyTypeKeys(entity.getCompanyTypeKeys())
                .tagKeys(entity.getTagKeys())
                .rating(entity.getRating())
                .notes(entity.getNotes())
                .piiStripped(entity.getPiiStripped())
                .legalHold(entity.getLegalHold())
                .archivedAt(entity.getArchivedAt())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

