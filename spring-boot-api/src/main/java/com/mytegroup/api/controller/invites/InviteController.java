package com.mytegroup.api.controller.invites;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.invites.*;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.core.InviteStatus;
import com.mytegroup.api.service.invites.InvitesService;
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
 * Invites controller.
 * Endpoints:
 * - GET /invites - List invites (Admin+)
 * - POST /invites - Create invite (Admin+) -> 201
 * - POST /invites/:id/resend - Resend invite (Admin+)
 * - POST /invites/:id/cancel - Cancel invite (Admin+)
 * - POST /invites/accept - Accept invite (Public with token)
 */
@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InvitesService invitesService;

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) InviteStatus status) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        List<Invite> invites = invitesService.list(orgId, status);
        
        return invites.stream()
            .map(this::inviteToMap)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> create(
            @RequestBody @Valid CreateInviteDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Role role = dto.getRole() != null ? dto.getRole() : Role.USER;
        int expiresInHours = dto.getExpiresInHours() != null ? dto.getExpiresInHours() : 72;
        
        Invite invite = invitesService.create(
            Long.parseLong(dto.getPersonId()),
            role,
            expiresInHours,
            orgId
        );
        
        return inviteToMap(invite);
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> resend(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Invite invite = invitesService.resend(id, orgId);
        
        return inviteToMap(invite);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Invite invite = invitesService.cancel(id, orgId);
        
        return inviteToMap(invite);
    }

    @PostMapping("/accept")
    public Map<String, Object> accept(@RequestBody @Valid AcceptInviteDto dto) {
        Map<String, Object> result = invitesService.accept(
            dto.getToken(),
            dto.getUsername(),
            dto.getPassword()
        );
        
        User user = (User) result.get("user");
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "username", user.getUsername(),
            "role", user.getRole().getValue(),
            "isEmailVerified", user.getIsEmailVerified()
        ));
        
        return response;
    }
    
    // Helper methods
    
    private Map<String, Object> inviteToMap(Invite invite) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", invite.getId());
        map.put("email", invite.getEmail());
        map.put("role", invite.getRole() != null ? invite.getRole().getValue() : null);
        map.put("status", invite.getStatus() != null ? invite.getStatus().name() : null);
        map.put("personId", invite.getPerson() != null ? invite.getPerson().getId() : null);
        map.put("tokenExpires", invite.getTokenExpires());
        map.put("acceptedAt", invite.getAcceptedAt());
        map.put("invitedUserId", invite.getInvitedUser() != null ? invite.getInvitedUser().getId() : null);
        map.put("createdByUserId", invite.getCreatedByUser() != null ? invite.getCreatedByUser().getId() : null);
        map.put("orgId", invite.getOrganization() != null ? invite.getOrganization().getId() : null);
        map.put("createdAt", invite.getCreatedAt());
        return map;
    }
    
}
