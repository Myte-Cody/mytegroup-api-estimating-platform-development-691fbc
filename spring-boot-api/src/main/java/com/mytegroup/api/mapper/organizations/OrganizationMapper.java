package com.mytegroup.api.mapper.organizations;

import com.mytegroup.api.dto.organizations.CreateOrganizationDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.organization.DataResidency;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    /**
     * Maps CreateOrganizationDto to Organization entity.
     */
    public Organization toEntity(CreateOrganizationDto dto, User ownerUser, User createdByUser) {
        Organization org = new Organization();
        org.setName(dto.name());
        org.setMetadata(dto.metadata());
        org.setDatabaseUri(dto.databaseUri());
        org.setDatastoreUri(dto.datastoreUri());
        org.setDatabaseName(dto.databaseName());
        org.setPrimaryDomain(dto.primaryDomain());
        org.setUseDedicatedDb(dto.useDedicatedDb() != null ? dto.useDedicatedDb() : false);
        org.setDatastoreType(dto.datastoreType() != null ? dto.datastoreType() : DatastoreType.SHARED);
        org.setDataResidency(dto.dataResidency() != null ? dto.dataResidency() : DataResidency.SHARED);
        org.setPiiStripped(dto.piiStripped() != null ? dto.piiStripped() : false);
        org.setLegalHold(dto.legalHold() != null ? dto.legalHold() : false);
        org.setOwnerUser(ownerUser);
        org.setCreatedByUser(createdByUser);
        
        return org;
    }

    /**
     * Updates existing Organization entity with UpdateOrganizationDto values.
     */
    public void updateEntity(Organization org, UpdateOrganizationDto dto) {
        if (dto.name() != null) {
            org.setName(dto.name());
        }
        if (dto.metadata() != null) {
            org.setMetadata(dto.metadata());
        }
    }
}

