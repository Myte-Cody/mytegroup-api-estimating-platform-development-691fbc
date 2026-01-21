package com.mytegroup.api.controller.rbac;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.users.UpdateUserRolesDto;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.RoleExpansionHelper;
import com.mytegroup.api.service.rbac.RbacService;
import com.mytegroup.api.service.users.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rbac")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;
    private final UsersService usersService;

    @GetMapping("/roles")
    public ResponseEntity<?> listRoles() {
        List<Map<String, Object>> roles = rbacService.listRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/roles/{role}/hierarchy")
    public ResponseEntity<?> getRoleHierarchy(@PathVariable String role) {
        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            List<Role> hierarchy = RoleExpansionHelper.expandRoles(List.of(roleEnum));
            
            Map<String, Object> response = new HashMap<>();
            response.put("role", role);
            response.put("hierarchy", hierarchy.stream().map(Role::getValue).toList());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + role));
        }
    }

    @PatchMapping("/users/{userId}/roles")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserRolesDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        User updatedUser = usersService.updateRoles(userId, dto.getRoles(), actor);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", updatedUser.getId());
        response.put("role", updatedUser.getRole() != null ? updatedUser.getRole().getValue() : null);
        response.put("roles", updatedUser.getRoles().stream().map(Role::getValue).toList());
        
        return ResponseEntity.ok(response);
    }
    
    private ActorContext getActorContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new ActorContext(null, null, null, null);
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
