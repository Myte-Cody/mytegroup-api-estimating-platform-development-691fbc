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
public class OrganizationResponseDto {
    private Long id;
    private String name;
    private String primaryDomain;
    private String datastoreType;
    private Map<String, Object> datastoreConfig;
    private String ownerId;
    private Boolean piiStripped;
    private Boolean legalHold;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


