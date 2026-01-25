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
public class PersonResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String primaryPhoneE164;
    private String title;
    private String notes;
    private Boolean piiStripped;
    private Boolean legalHold;
    private LocalDateTime archivedAt;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


