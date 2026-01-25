package com.mytegroup.api.mapper.organizations;

import com.mytegroup.api.dto.organizations.CreateOrganizationDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationDto;
import com.mytegroup.api.dto.response.OrganizationResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.organization.DataResidency;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
        // Note: datastoreUri is not a field in Organization entity, only databaseUri exists
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

    /**
     * Maps Organization entity to OrganizationResponseDto.
     */
    public OrganizationResponseDto toDto(Organization entity) {
        if (entity == null) {
            return null;
        }
        
        Map<String, Object> datastoreConfig = new HashMap<>();
        datastoreConfig.put("databaseUri", entity.getDatabaseUri());
        datastoreConfig.put("databaseName", entity.getDatabaseName());
        
        return OrganizationResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .primaryDomain(entity.getPrimaryDomain())
                .datastoreType(entity.getDatastoreType() != null ? entity.getDatastoreType().getValue() : null)
                .datastoreConfig(datastoreConfig)
                .ownerId(entity.getOwnerUser() != null ? entity.getOwnerUser().getId().toString() : null)
                .piiStripped(entity.getPiiStripped())
                .legalHold(entity.getLegalHold())
                .archivedAt(entity.getArchivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

