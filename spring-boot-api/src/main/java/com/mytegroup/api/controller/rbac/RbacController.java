package com.mytegroup.api.controller.rbac;

import com.mytegroup.api.dto.users.UpdateUserRolesDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rbac")
@PreAuthorize("isAuthenticated()")
public class RbacController {

    @PatchMapping("/users/{userId}/roles")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> updateUserRoles(@PathVariable String userId, @RequestBody @Valid UpdateUserRolesDto dto) {
        return ResponseEntity.ok().build();
    }
}
