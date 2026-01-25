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
public class InviteResponseDto {
    private Long id;
    private String personId;
    private String role;
    private LocalDateTime tokenExpires;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
}


