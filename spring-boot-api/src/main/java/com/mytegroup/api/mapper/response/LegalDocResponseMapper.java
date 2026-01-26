package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.LegalDocResponseDto;
import com.mytegroup.api.entity.legal.LegalDoc;
import org.springframework.stereotype.Component;

@Component
public class LegalDocResponseMapper {
    public LegalDocResponseDto toDto(LegalDoc entity) {
        if (entity == null) {
            return null;
        }
        
        return LegalDocResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType() != null ? entity.getType().getValue() : null)
                .version(entity.getVersion())
                .content(entity.getContent())
                .effectiveAt(entity.getEffectiveAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}



