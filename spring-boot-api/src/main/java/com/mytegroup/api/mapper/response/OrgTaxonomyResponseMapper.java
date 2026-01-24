package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.OrgTaxonomyResponseDto;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrgTaxonomyResponseMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public OrgTaxonomyResponseDto toDto(OrgTaxonomy entity) {
        if (entity == null) {
            return null;
        }
        
        // Parse the metadata JSON string to Map
        Map<String, Object> values = new HashMap<>();
        if (entity.getMetadata() != null && !entity.getMetadata().isEmpty()) {
            try {
                values = objectMapper.readValue(entity.getMetadata(), Map.class);
            } catch (Exception e) {
                values = new HashMap<>();
            }
        }
        
        return OrgTaxonomyResponseDto.builder()
                .id(entity.getId())
                .namespace(entity.getNamespace())
                .values(values)
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

