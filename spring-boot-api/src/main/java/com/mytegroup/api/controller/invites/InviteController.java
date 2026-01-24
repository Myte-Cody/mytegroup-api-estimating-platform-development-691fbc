package com.mytegroup.api.controller.invites;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.invites.*;
import com.mytegroup.api.dto.response.InviteResponseDto;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.core.InviteStatus;
import com.mytegroup.api.mapper.response.InviteResponseMapper;
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
    private final InviteResponseMapper inviteResponseMapper;

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<InviteResponseDto> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) InviteStatus status) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        List<Invite> invites = invitesService.list(orgId, status);
        
        return invites.stream()
            .map(inviteResponseMapper::toDto)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public InviteResponseDto create(
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
        
        return inviteResponseMapper.toDto(invite);
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public InviteResponseDto resend(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Invite invite = invitesService.resend(id, orgId);
        
        return inviteResponseMapper.toDto(invite);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public InviteResponseDto cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Invite invite = invitesService.cancel(id, orgId);
        
        return inviteResponseMapper.toDto(invite);
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
    
}
