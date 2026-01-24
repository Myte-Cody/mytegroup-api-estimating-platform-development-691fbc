package com.mytegroup.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateResponseDto {
    private Long id;
    private Long projectId;
    private String estimateName;
    private String description;
    private String status;
    private List<Map<String, Object>> lineItems;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

