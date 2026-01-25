package com.mytegroup.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdgeResponseDto {
    private Long id;
    private String fromNodeType;
    private Long fromNodeId;
    private String toNodeType;
    private Long toNodeId;
    private String edgeTypeKey;
    private Map<String, Object> metadata;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String orgId;
    private LocalDateTime createdAt;
}


