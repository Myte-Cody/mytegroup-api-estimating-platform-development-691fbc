package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.NotificationResponseDto;
import com.mytegroup.api.entity.notifications.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationResponseMapper {
    public NotificationResponseDto toDto(Notification entity) {
        if (entity == null) {
            return null;
        }
        
        return NotificationResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .isRead(entity.getIsRead())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

