package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.SeatResponseDto;
import com.mytegroup.api.entity.seats.Seat;
import org.springframework.stereotype.Component;

@Component
public class SeatResponseMapper {
    public SeatResponseDto toDto(Seat entity) {
        if (entity == null) {
            return null;
        }
        
        return SeatResponseDto.builder()
                .id(entity.getId())
                .seatType(entity.getSeatType())
                .quantity(entity.getQuantity())
                .isActive(entity.getIsActive())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

