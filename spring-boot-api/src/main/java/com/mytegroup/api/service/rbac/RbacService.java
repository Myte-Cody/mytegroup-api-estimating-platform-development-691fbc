package com.mytegroup.api.service.rbac;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.RoleExpansionHelper;
import com.mytegroup.api.service.users.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for role-based access control.
 * Handles role hierarchy, user role management, and role assignment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacService {
    
    private final UsersService usersService;
    
    /**
     * Returns the role hierarchy structure
     */
    @Transactional(readOnly = true)
    public Map<String, Object> hierarchy() {
        // TODO: Return role hierarchy from RoleExpansionHelper
        // This would require exposing ROLE_HIERARCHY and ROLE_PRIORITY from RoleExpansionHelper
        Map<String, Object> result = new HashMap<>();
        result.put("roles", Role.values());
        result.put("hierarchy", "TODO: Expose from RoleExpansionHelper");
        result.put("priority", "TODO: Expose from RoleExpansionHelper");
        return result;
    }
    
    /**
     * Gets roles for a user
     */
    @Transactional(readOnly = true)
    public User getUserRoles(Long userId, ActorContext actor) {
        return usersService.getById(userId, actor, false);
    }
    
    /**
     * Lists user roles for an organization
     */
    @Transactional(readOnly = true)
    public List<User> listUserRoles(String orgId, ActorContext actor) {
        return usersService.list(actor, orgId, false);
    }
    
    /**
     * Updates user roles
     */
    @Transactional
    public User updateUserRoles(Long userId, List<Role> roles, ActorContext actor) {
        if (roles == null || roles.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        
        User user = usersService.getById(userId, actor, false);
        
        // TODO: Implement role update logic in UsersService
        // This requires updating the user's roles list and primary role
        
        return user;
    }
    
    /**
     * Revokes a specific role from a user
     */
    @Transactional
    public User revokeRole(Long userId, Role role, ActorContext actor) {
        User user = getUserRoles(userId, actor);
        
        List<Role> currentRoles = user.getRoles();
        if (currentRoles == null || currentRoles.isEmpty()) {
            currentRoles = List.of(user.getRole() != null ? user.getRole() : Role.USER);
        }
        
        List<Role> remaining = currentRoles.stream()
            .filter(r -> r != role)
            .toList();
        
        if (remaining.isEmpty()) {
            throw new BadRequestException("User must retain at least one role");
        }
        
        return updateUserRoles(userId, remaining, actor);
    }
}

