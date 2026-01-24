package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.UserResponseDto;
import com.mytegroup.api.entity.people.User;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {
    public UserResponseDto toDto(User entity) {
        if (entity == null) {
            return null;
        }
        
        return UserResponseDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .role(entity.getRole() != null ? entity.getRole().getValue() : null)
                .emailVerified(entity.getEmailVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

