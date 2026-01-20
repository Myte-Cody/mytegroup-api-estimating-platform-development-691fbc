package com.mytegroup.api.controller.users;

import com.mytegroup.api.dto.users.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Users controller.
 * Endpoints:
 * - GET /users - List users (Admin+)
 * - POST /users - Create user (Admin+)
 * - GET /users/:id - Get user (Admin+)
 * - PATCH /users/:id - Update user (Admin+)
 * - PATCH /users/:id/roles - Update roles (Admin+)
 * - POST /users/:id/archive - Archive user (Admin+)
 * - POST /users/:id/unarchive - Unarchive user (Admin+)
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

    // TODO: Inject UserService, UserMapper

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@ModelAttribute ListUsersQueryDto query) {
        // TODO: Implement list users logic
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(@RequestBody @Valid CreateUserDto dto) {
        // TODO: Implement create user logic
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(@PathVariable String id, @ModelAttribute GetUserQueryDto query) {
        // TODO: Implement get user by id logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody @Valid UpdateUserDto dto) {
        // TODO: Implement update user logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> updateRoles(@PathVariable String id, @RequestBody @Valid UpdateUserRolesDto dto) {
        // TODO: Implement update user roles logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(@PathVariable String id) {
        // TODO: Implement archive user logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(@PathVariable String id) {
        // TODO: Implement unarchive user logic
        return ResponseEntity.ok().build();
    }
}

