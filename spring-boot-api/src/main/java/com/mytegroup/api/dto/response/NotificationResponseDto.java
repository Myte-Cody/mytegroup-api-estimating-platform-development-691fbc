package com.mytegroup.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private Long id;
    private String userId;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private Object metadata;
    private LocalDateTime createdAt;
}


