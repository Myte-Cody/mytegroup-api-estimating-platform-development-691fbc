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
public class OfficeResponseDto {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String city;
    private String region;
    private String postal;
    private String country;
    private String orgId;
    private Boolean isActive;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

