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
public class ContactResponseDto {
    private Long id;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String personType;
    private String notes;
    private String ironworkerNumber;
    private Object company;
    private Boolean piiStripped;
    private Boolean legalHold;
    private LocalDateTime archivedAt;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



