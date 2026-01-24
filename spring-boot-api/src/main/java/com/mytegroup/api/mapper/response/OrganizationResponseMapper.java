package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.OrganizationResponseDto;
import com.mytegroup.api.entity.core.Organization;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrganizationResponseMapper {
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
                .ownerId(entity.getOwnerUser() != null ? entity.getOwnerUser().getId() : null)
                .piiStripped(entity.getPiiStripped())
                .legalHold(entity.getLegalHold())
                .archivedAt(entity.getArchivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

