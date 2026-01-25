package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.OrgTaxonomyResponseDto;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
import org.springframework.stereotype.Component;

@Component
public class OrgTaxonomyResponseMapper {
    
    public OrgTaxonomyResponseDto toDto(OrgTaxonomy entity) {
        if (entity == null) {
            return null;
        }
        
        return OrgTaxonomyResponseDto.builder()
                .id(entity.getId())
                .namespace(entity.getNamespace())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

