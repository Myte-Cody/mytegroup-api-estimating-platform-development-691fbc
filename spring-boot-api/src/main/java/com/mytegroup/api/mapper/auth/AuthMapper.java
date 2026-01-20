package com.mytegroup.api.mapper.auth;

import com.mytegroup.api.dto.auth.LoginDto;
import com.mytegroup.api.dto.auth.RegisterDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthMapper {

    /**
     * Maps RegisterDto to User entity.
     * Note: passwordHash should be set by the service layer after hashing.
     */
    public User toEntity(RegisterDto dto, Organization organization) {
        User user = new User();
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setOrganization(organization);
        
        // Set role - use provided role or default to USER
        if (dto.role() != null) {
            user.setRole(dto.role());
            List<com.mytegroup.api.common.enums.Role> roles = new ArrayList<>();
            roles.add(dto.role());
            user.setRoles(roles);
        } else {
            user.setRole(com.mytegroup.api.common.enums.Role.USER);
            user.setRoles(new ArrayList<>(List.of(com.mytegroup.api.common.enums.Role.USER)));
        }
        
        user.setIsEmailVerified(false);
        user.setIsOrgOwner(false);
        user.setPiiStripped(false);
        user.setLegalHold(false);
        
        return user;
    }

    /**
     * Extracts email and password from LoginDto for authentication.
     * Returns a simple object with email and password for the service layer.
     */
    public LoginCredentials toLoginCredentials(LoginDto dto) {
        return new LoginCredentials(dto.email(), dto.password());
    }

    /**
     * Simple record for login credentials.
     */
    public record LoginCredentials(String email, String password) {
    }
}

