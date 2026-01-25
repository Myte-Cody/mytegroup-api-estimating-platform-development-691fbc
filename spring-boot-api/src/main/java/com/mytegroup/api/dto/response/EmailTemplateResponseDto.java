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
public class EmailTemplateResponseDto {
    private Long id;
    private String name;
    private String subject;
    private String body;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


