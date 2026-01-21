package com.mytegroup.api.controller.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.users.*;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.users.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        List<User> users = usersService.list(actor, orgId, includeArchived);
        
        return users.stream()
            .map(this::userToMap)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> create(@RequestBody @Valid CreateUserDto dto) {
        ActorContext actor = getActorContext();
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(dto.getPassword()); // Will be hashed in service
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getRoles() != null) {
            user.setRoles(dto.getRoles());
        }
        
        User savedUser = usersService.create(user, actor, false);
        
        return userToMap(savedUser);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> getById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        User user = usersService.getById(id, actor, includeArchived);
        
        return userToMap(user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody @Valid UpdateUserDto dto) {
        ActorContext actor = getActorContext();
        
        User userUpdates = new User();
        if (dto.getUsername() != null) {
            userUpdates.setUsername(dto.getUsername());
        }
        if (dto.getEmail() != null) {
            userUpdates.setEmail(dto.getEmail());
        }
        if (dto.getIsEmailVerified() != null) {
            userUpdates.setIsEmailVerified(dto.getIsEmailVerified());
        }
        if (dto.getPiiStripped() != null) {
            userUpdates.setPiiStripped(dto.getPiiStripped());
        }
        if (dto.getLegalHold() != null) {
            userUpdates.setLegalHold(dto.getLegalHold());
        }
        
        User updatedUser = usersService.update(id, userUpdates, actor);
        
        return userToMap(updatedUser);
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> updateRoles(@PathVariable Long id, @RequestBody @Valid UpdateUserRolesDto dto) {
        ActorContext actor = getActorContext();
        
        User updatedUser = usersService.updateRoles(id, dto.getRoles(), actor);
        
        return userToMap(updatedUser);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> archive(@PathVariable Long id) {
        ActorContext actor = getActorContext();
        
        User archivedUser = usersService.archive(id, actor);
        
        return userToMap(archivedUser);
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> unarchive(@PathVariable Long id) {
        ActorContext actor = getActorContext();
        
        User unarchivedUser = usersService.unarchive(id, actor);
        
        return userToMap(unarchivedUser);
    }
    
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> getRoles(@PathVariable Long id) {
        ActorContext actor = getActorContext();
        
        return usersService.getUserRoles(id, actor);
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
    
    private ActorContext getActorContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        
        Long userId = null;
        if (auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        } else if (auth.getPrincipal() instanceof String) {
            try {
                userId = Long.parseLong((String) auth.getPrincipal());
            } catch (NumberFormatException ignored) {}
        }
        
        return new ActorContext(
            userId != null ? userId.toString() : null,
            null,
            null,
            null
        );
    }
}
