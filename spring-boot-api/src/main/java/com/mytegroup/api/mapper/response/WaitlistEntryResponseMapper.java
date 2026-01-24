package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.WaitlistEntryResponseDto;
import com.mytegroup.api.entity.waitlist.WaitlistEntry;
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
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .company(entity.getCompany())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

