package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.NotificationResponseDto;
import com.mytegroup.api.entity.communication.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationResponseMapper {
    public NotificationResponseDto toDto(Notification entity) {
        if (entity == null) {
            return null;
        }
        
        return NotificationResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId().toString() : null)
                .type(entity.getType())
                .metadata(entity.getPayload())
                .isRead(entity.getRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

