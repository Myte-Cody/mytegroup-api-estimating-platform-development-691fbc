package com.mytegroup.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetResponseDto {
    private Long id;
    private String orgId;
    private String projectId;
    private String personId;
    private String crewId;
    private LocalDate date;
    private String status;
    private String createdBy;
    private LocalDateTime submittedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private List<TimesheetEntryResponseDto> entries;
    private LocalDateTime archivedAt;
    private Boolean piiStripped;
    private Boolean legalHold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
