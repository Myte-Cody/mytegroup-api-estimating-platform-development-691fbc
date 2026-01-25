package com.mytegroup.api.mapper.companylocations;

import com.mytegroup.api.dto.companylocations.CreateCompanyLocationDto;
import com.mytegroup.api.dto.companylocations.UpdateCompanyLocationDto;
import com.mytegroup.api.dto.response.CompanyLocationResponseDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class CompanyLocationMapper {

    public CompanyLocation toEntity(CreateCompanyLocationDto dto, Organization organization, Company company) {
        CompanyLocation location = new CompanyLocation();
        location.setOrganization(organization);
        location.setCompany(company);
        location.setName(dto.name());
        location.setExternalId(dto.externalId());
        location.setTimezone(dto.timezone());
        location.setEmail(dto.email());
        location.setPhone(dto.phone());
        location.setAddressLine1(dto.addressLine1());
        location.setAddressLine2(dto.addressLine2());
        location.setCity(dto.city());
        location.setRegion(dto.region());
        location.setPostal(dto.postal());
        location.setCountry(dto.country());
        location.setTagKeys(dto.tagKeys() != null ? new ArrayList<>(dto.tagKeys()) : new ArrayList<>());
        location.setNotes(dto.notes());
        return location;
    }

    public void updateEntity(CompanyLocation location, UpdateCompanyLocationDto dto) {
        if (dto.name() != null) {
            location.setName(dto.name());
        }
        if (dto.externalId() != null) {
            location.setExternalId(dto.externalId());
        }
        if (dto.timezone() != null) {
            location.setTimezone(dto.timezone());
        }
        if (dto.email() != null) {
            location.setEmail(dto.email());
        }
        if (dto.phone() != null) {
            location.setPhone(dto.phone());
        }
        if (dto.addressLine1() != null) {
            location.setAddressLine1(dto.addressLine1());
        }
        if (dto.addressLine2() != null) {
            location.setAddressLine2(dto.addressLine2());
        }
        if (dto.city() != null) {
            location.setCity(dto.city());
        }
        if (dto.region() != null) {
            location.setRegion(dto.region());
        }
        if (dto.postal() != null) {
            location.setPostal(dto.postal());
        }
        if (dto.country() != null) {
            location.setCountry(dto.country());
        }
        if (dto.tagKeys() != null) {
            location.setTagKeys(new ArrayList<>(dto.tagKeys()));
        }
        if (dto.notes() != null) {
            location.setNotes(dto.notes());
        }
    }

    /**
     * Maps CompanyLocation entity to CompanyLocationResponseDto.
     */
    public CompanyLocationResponseDto toDto(CompanyLocation entity) {
        if (entity == null) {
            return null;
        }
        
        return CompanyLocationResponseDto.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .address(entity.getAddressLine1())
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

