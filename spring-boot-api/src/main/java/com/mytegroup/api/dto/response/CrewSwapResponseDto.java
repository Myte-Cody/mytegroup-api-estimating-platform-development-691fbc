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
public class CrewSwapResponseDto {
    private Long id;
    private String orgId;
    private String projectId;
    private String personId;
    private String fromCrewId;
    private String toCrewId;
    private String status;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private String completedBy;
    private LocalDateTime completedAt;
    private LocalDateTime archivedAt;
    private Boolean piiStripped;
    private Boolean legalHold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
