package com.mytegroup.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewAssignmentResponseDto {
    private Long id;
    private String orgId;
    private String projectId;
    private String personId;
    private String crewId;
    private String roleKey;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String createdBy;
    private LocalDateTime archivedAt;
    private Boolean piiStripped;
    private Boolean legalHold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
