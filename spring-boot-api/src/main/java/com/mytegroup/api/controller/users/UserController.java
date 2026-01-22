package com.mytegroup.api.controller.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.users.*;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.mapper.users.UserMapper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.users.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Users controller.
 * Endpoints:
 * - GET /users - List users (Admin+)
 * - POST /users - Create user (Admin+) -> 201
 * - GET /users/:id - Get user (Admin+)
 * - PATCH /users/:id - Update user (Admin+)
 * - PATCH /users/:id/roles - Update roles (Admin+)
 * - POST /users/:id/archive - Archive user (Admin+)
 * - POST /users/:id/unarchive - Unarchive user (Admin+)
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class UserController {

    private final UsersService usersService;
    private final UserMapper userMapper;
    private final ServiceAuthorizationHelper authHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        if (orgId == null) {
            return List.of();
        }
        
        List<User> users = usersService.list(orgId, includeArchived);
        
        return users.stream()
            .map(this::userToMap)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> create(@RequestBody @Valid CreateUserDto dto) {
        // Get organization from DTO
        if (dto.getOrgId() == null) {
            return Map.of("error", "orgId is required");
        }
        Organization organization = authHelper.validateOrg(dto.getOrgId());
        
        // Use mapper to create user entity
        User user = userMapper.toEntity(dto, organization);
        // Set password hash (will be hashed in service)
        if (dto.getPassword() != null) {
            user.setPasswordHash(dto.getPassword());
        }
        
        User savedUser = usersService.create(user);
        
        return userToMap(savedUser);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> getById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        User user = usersService.getById(id, includeArchived);
        
        return userToMap(user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody @Valid UpdateUserDto dto) {
        // Create a User object with updates using mapper
        User userUpdates = new User();
        userMapper.updateEntity(userUpdates, dto);
        
        User updatedUser = usersService.update(id, userUpdates);
        
        return userToMap(updatedUser);
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> updateRoles(@PathVariable Long id, @RequestBody @Valid UpdateUserRolesDto dto) {
        User updatedUser = usersService.updateRoles(id, dto.getRoles());
        
        return userToMap(updatedUser);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> archive(@PathVariable Long id) {
        User archivedUser = usersService.archive(id);
        
        return userToMap(archivedUser);
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> unarchive(@PathVariable Long id) {
        User unarchivedUser = usersService.unarchive(id);
        
        return userToMap(unarchivedUser);
    }
    
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> getRoles(@PathVariable Long id) {
        return usersService.getUserRoles(id);
    }
    
    // Helper methods
    
    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("username", user.getUsername());
        map.put("firstName", user.getFirstName());
        map.put("lastName", user.getLastName());
        map.put("role", user.getRole() != null ? user.getRole().getValue() : null);
        map.put("roles", user.getRoles() != null 
            ? user.getRoles().stream().map(Role::getValue).toList() 
            : List.of());
        map.put("isEmailVerified", user.getIsEmailVerified());
        map.put("isOrgOwner", user.getIsOrgOwner());
        map.put("archivedAt", user.getArchivedAt());
        map.put("lastLogin", user.getLastLogin());
        map.put("piiStripped", user.getPiiStripped());
        map.put("legalHold", user.getLegalHold());
        map.put("orgId", user.getOrganization() != null ? user.getOrganization().getId() : null);
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        return map;
    }
    
}
