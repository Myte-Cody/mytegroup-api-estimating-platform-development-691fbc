package com.mytegroup.api.mapper.companies;

import com.mytegroup.api.dto.companies.CreateCompanyDto;
import com.mytegroup.api.dto.companies.UpdateCompanyDto;
import com.mytegroup.api.dto.response.CompanyResponseDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    /**
     * Maps CreateCompanyDto to Company entity.
     */
    public Company toEntity(CreateCompanyDto dto, Organization organization) {
        Company company = new Company();
        company.setName(dto.name());
        company.setExternalId(dto.externalId());
        company.setWebsite(dto.website());
        company.setMainEmail(dto.mainEmail());
        company.setMainPhone(dto.mainPhone());
        company.setCompanyTypeKeys(dto.companyTypeKeys());
        company.setTagKeys(dto.tagKeys());
        company.setRating(dto.rating());
        company.setNotes(dto.notes());
        company.setOrganization(organization);
        
        return company;
    }

    /**
     * Updates existing Company entity with UpdateCompanyDto values.
     */
    public void updateEntity(Company company, UpdateCompanyDto dto) {
        if (dto.name() != null) {
            company.setName(dto.name());
        }
        if (dto.externalId() != null) {
            company.setExternalId(dto.externalId());
        }
        if (dto.website() != null) {
            company.setWebsite(dto.website());
        }
        if (dto.mainEmail() != null) {
            company.setMainEmail(dto.mainEmail());
        }
        if (dto.mainPhone() != null) {
            company.setMainPhone(dto.mainPhone());
        }
        if (dto.companyTypeKeys() != null) {
            company.setCompanyTypeKeys(dto.companyTypeKeys());
        }
        if (dto.tagKeys() != null) {
            company.setTagKeys(dto.tagKeys());
        }
        if (dto.rating() != null) {
            company.setRating(dto.rating());
        }
        if (dto.notes() != null) {
            company.setNotes(dto.notes());
        }
    }

    /**
     * Maps Company entity to CompanyResponseDto.
     */
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
                    .rating(entity.getRating() != null ? entity.getRating().toString() : null)
                .notes(entity.getNotes())
                .piiStripped(entity.getPiiStripped())
                .legalHold(entity.getLegalHold())
                .archivedAt(entity.getArchivedAt())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

