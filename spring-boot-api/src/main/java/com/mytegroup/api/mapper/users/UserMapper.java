package com.mytegroup.api.mapper.users;

import com.mytegroup.api.dto.users.CreateUserDto;
import com.mytegroup.api.dto.users.UpdateUserDto;
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
        user.setUsername(dto.username());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setOrganization(organization);
        
        // Set role - use provided role or default to USER
        if (dto.role() != null) {
            user.setRole(dto.role());
            List<com.mytegroup.api.common.enums.Role> roles = dto.roles() != null && !dto.roles().isEmpty() 
                ? new ArrayList<>(dto.roles()) 
                : new ArrayList<>(List.of(dto.role()));
            user.setRoles(roles);
        } else {
            user.setRole(com.mytegroup.api.common.enums.Role.USER);
            user.setRoles(new ArrayList<>(List.of(com.mytegroup.api.common.enums.Role.USER)));
        }
        
        user.setVerificationTokenHash(dto.verificationTokenHash());
        user.setVerificationTokenExpires(dto.verificationTokenExpires());
        user.setResetTokenHash(dto.resetTokenHash());
        user.setResetTokenExpires(dto.resetTokenExpires());
        user.setIsEmailVerified(dto.isEmailVerified() != null ? dto.isEmailVerified() : false);
        user.setIsOrgOwner(dto.isOrgOwner() != null ? dto.isOrgOwner() : false);
        user.setPiiStripped(false);
        user.setLegalHold(false);
        
        return user;
    }

    /**
     * Updates existing User entity with UpdateUserDto values.
     */
    public void updateEntity(User user, UpdateUserDto dto) {
        if (dto.username() != null) {
            user.setUsername(dto.username());
        }
        if (dto.firstName() != null) {
            user.setFirstName(dto.firstName());
        }
        if (dto.lastName() != null) {
            user.setLastName(dto.lastName());
        }
        if (dto.email() != null) {
            user.setEmail(dto.email());
        }
        if (dto.isEmailVerified() != null) {
            user.setIsEmailVerified(dto.isEmailVerified());
        }
        if (dto.piiStripped() != null) {
            user.setPiiStripped(dto.piiStripped());
        }
        if (dto.legalHold() != null) {
            user.setLegalHold(dto.legalHold());
        }
    }
}

