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
public class LegalDocResponseDto {
    private Long id;
    private String type;
    private String version;
    private String content;
    private LocalDateTime effectiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

