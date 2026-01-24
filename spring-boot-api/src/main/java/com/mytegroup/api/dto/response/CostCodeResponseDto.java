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
public class CostCodeResponseDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

