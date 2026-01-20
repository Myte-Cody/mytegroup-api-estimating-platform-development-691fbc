package com.mytegroup.api.mapper.offices;

import com.mytegroup.api.dto.offices.CreateOfficeDto;
import com.mytegroup.api.dto.offices.UpdateOfficeDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class OfficeMapper {

    public Office toEntity(CreateOfficeDto dto, Organization organization, Office parent) {
        Office office = new Office();
        office.setName(dto.name());
        office.setOrganization(organization);
        office.setAddress(dto.address());
        office.setDescription(dto.description());
        office.setTimezone(dto.timezone());
        office.setOrgLocationTypeKey(dto.orgLocationTypeKey());
        office.setTagKeys(dto.tagKeys() != null ? new ArrayList<>(dto.tagKeys()) : new ArrayList<>());
        office.setParent(parent);
        office.setSortOrder(dto.sortOrder());
        return office;
    }

    public void updateEntity(Office office, UpdateOfficeDto dto, Office parent) {
        if (dto.name() != null) {
            office.setName(dto.name());
        }
        if (dto.address() != null) {
            office.setAddress(dto.address());
        }
        if (dto.description() != null) {
            office.setDescription(dto.description());
        }
        if (dto.timezone() != null) {
            office.setTimezone(dto.timezone());
        }
        if (dto.orgLocationTypeKey() != null) {
            office.setOrgLocationTypeKey(dto.orgLocationTypeKey());
        }
        if (dto.tagKeys() != null) {
            office.setTagKeys(new ArrayList<>(dto.tagKeys()));
        }
        if (parent != null) {
            office.setParent(parent);
        }
        if (dto.sortOrder() != null) {
            office.setSortOrder(dto.sortOrder());
        }
    }
}

