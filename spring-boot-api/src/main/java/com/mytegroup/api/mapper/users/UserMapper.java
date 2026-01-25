package com.mytegroup.api.mapper.users;

import com.mytegroup.api.dto.users.CreateUserDto;
import com.mytegroup.api.dto.users.UpdateUserDto;
import com.mytegroup.api.dto.response.UserResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserMapper {

    /**
     * Maps CreateUserDto to User entity.
     * Note: passwordHash should be set by the service layer after hashing.
     */
    public User toEntity(CreateUserDto dto, Organization organization) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setOrganization(organization);
        
        // Set role - use provided role or default to USER
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
            List<com.mytegroup.api.common.enums.Role> roles = dto.getRoles() != null && !dto.getRoles().isEmpty() 
                ? new ArrayList<>(dto.getRoles()) 
                : new ArrayList<>(List.of(dto.getRole()));
            user.setRoles(roles);
        } else {
            user.setRole(com.mytegroup.api.common.enums.Role.USER);
            user.setRoles(new ArrayList<>(List.of(com.mytegroup.api.common.enums.Role.USER)));
        }
        
        user.setVerificationTokenHash(dto.getVerificationTokenHash());
        user.setVerificationTokenExpires(dto.getVerificationTokenExpires());
        user.setResetTokenHash(dto.getResetTokenHash());
        user.setResetTokenExpires(dto.getResetTokenExpires());
        user.setIsEmailVerified(dto.getIsEmailVerified() != null ? dto.getIsEmailVerified() : false);
        user.setIsOrgOwner(dto.getIsOrgOwner() != null ? dto.getIsOrgOwner() : false);
        user.setPiiStripped(false);
        user.setLegalHold(false);
        
        return user;
    }

    /**
     * Updates existing User entity with UpdateUserDto values.
     */
    public void updateEntity(User user, UpdateUserDto dto) {
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getIsEmailVerified() != null) {
            user.setIsEmailVerified(dto.getIsEmailVerified());
        }
        if (dto.getPiiStripped() != null) {
            user.setPiiStripped(dto.getPiiStripped());
        }
        if (dto.getLegalHold() != null) {
            user.setLegalHold(dto.getLegalHold());
        }
    }

    /**
     * Maps User entity to UserResponseDto.
     */
    public UserResponseDto toDto(User entity) {
        if (entity == null) {
            return null;
        }
        
        return UserResponseDto.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .username(entity.getUsername())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .role(entity.getRole() != null ? entity.getRole().getValue() : null)
                .emailVerified(entity.getIsEmailVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

