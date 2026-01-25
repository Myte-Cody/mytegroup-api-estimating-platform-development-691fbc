package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.WaitlistEntryResponseDto;
import com.mytegroup.api.entity.core.WaitlistEntry;
import org.springframework.stereotype.Component;

@Component
public class WaitlistEntryResponseMapper {
    public WaitlistEntryResponseDto toDto(WaitlistEntry entity) {
        if (entity == null) {
            return null;
        }
        
        return WaitlistEntryResponseDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .firstName(entity.getName())
                .status(entity.getStatus() != null ? entity.getStatus().getValue() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

