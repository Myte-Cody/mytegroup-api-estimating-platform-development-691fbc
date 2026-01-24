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
public class SeatResponseDto {
    private Long id;
    private String seatType;
    private Integer quantity;
    private Boolean isActive;
    private String orgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

