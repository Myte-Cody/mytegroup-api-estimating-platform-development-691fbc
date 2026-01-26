package com.mytegroup.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDto {
    private Long id;
    private String name;
    private String normalizedName;
    private String externalId;
    private String website;
    private String mainEmail;
    private String mainPhone;
    private List<String> companyTypeKeys;
    private List<String> tagKeys;
    private String rating;
    private String notes;
    private Boolean piiStripped;
    private Boolean legalHold;
    private LocalDateTime archivedAt;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



