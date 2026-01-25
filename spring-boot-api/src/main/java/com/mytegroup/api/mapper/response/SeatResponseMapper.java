package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.SeatResponseDto;
import com.mytegroup.api.entity.projects.Seat;
import org.springframework.stereotype.Component;

@Component
public class SeatResponseMapper {
    public SeatResponseDto toDto(Seat entity) {
        if (entity == null) {
            return null;
        }
        
        return SeatResponseDto.builder()
                .id(entity.getId())
                .seatType(entity.getRole())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

