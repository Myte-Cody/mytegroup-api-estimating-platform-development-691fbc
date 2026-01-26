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
public class ProjectResponseDto {
    private Long id;
    private String name;
    private String description;
    private String companyId;
    private String officeId;
    private String projectManager;
    private String status;
    private Boolean isActive;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



