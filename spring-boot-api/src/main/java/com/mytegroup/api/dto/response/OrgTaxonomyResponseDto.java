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
public class OrgTaxonomyResponseDto {
    private Long id;
    private String namespace;
    private Map<String, Object> values;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

