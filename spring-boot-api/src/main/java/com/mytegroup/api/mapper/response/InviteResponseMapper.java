package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.InviteResponseDto;
import com.mytegroup.api.entity.core.Invite;
import org.springframework.stereotype.Component;

@Component
public class InviteResponseMapper {
    public InviteResponseDto toDto(Invite entity) {
        if (entity == null) {
            return null;
        }
        
        return InviteResponseDto.builder()
                .id(entity.getId())
                .personId(entity.getPerson() != null ? entity.getPerson().getId().toString() : null)
                .role(entity.getRole() != null ? entity.getRole().getValue() : null)
                .tokenExpires(entity.getTokenExpires())
                .acceptedAt(entity.getAcceptedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

