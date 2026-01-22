package com.mytegroup.api.controller.auth;

import com.mytegroup.api.dto.auth.*;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.UnauthorizedException;
import com.mytegroup.api.security.JwtTokenProvider;
import com.mytegroup.api.service.auth.AuthService;
import com.mytegroup.api.service.users.UsersService;
import jakarta.servlet.http.HttpServletRequest;
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
 * Authentication controller.
 * Endpoints:
 * - POST /auth/register - Public registration -> 201
 * - POST /auth/login - Public login
 * - POST /auth/logout - Authenticated logout
 * - GET /auth/me - Authenticated get current user
 * - POST /auth/verify-email - Public verify email
 * - POST /auth/password-strength - Public check password strength
 * - POST /auth/forgot-password - Public forgot password
 * - POST /auth/reset-password - Public reset password
 * - GET /auth/users - Admin+ list users
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsersService usersService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody @Valid RegisterDto dto) {
        User user = authService.register(
            dto.getEmail(),
            dto.getPassword(),
            dto.getUsername(),
            dto.getFirstName(),
            dto.getLastName(),
            dto.getOrganizationName(),
            dto.getOrgId(),
            Boolean.TRUE.equals(dto.getLegalAccepted()),
            dto.getInviteToken()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("isEmailVerified", user.getIsEmailVerified());
        
        return response;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid LoginDto dto) {
        User user = authService.login(dto.getEmail(), dto.getPassword());
        
        // Generate JWT token - convert User to UserDetails
        org.springframework.security.core.userdetails.UserDetails userDetails = 
            org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(user.getRoles() != null ? user.getRoles().stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.getValue()))
                    .toArray(org.springframework.security.core.GrantedAuthority[]::new)
                    : new org.springframework.security.core.GrantedAuthority[0])
                .build();
        String token = jwtTokenProvider.generateToken(userDetails);
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "username", user.getUsername(),
            "role", user.getRole().getValue(),
            "roles", user.getRoles().stream().map(r -> r.getValue()).toList(),
            "isEmailVerified", user.getIsEmailVerified(),
            "orgId", user.getOrganization() != null ? user.getOrganization().getId() : null
        ));
        
        return response;
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> logout(HttpServletRequest request) {
        // JWT is stateless, so logout is handled client-side
        // Could optionally blacklist token here
        return Map.of("status", "ok");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        
        Long userId = extractUserId(auth);
        if (userId == null) {
            throw new UnauthorizedException("Invalid authentication");
        }
        
        User user = usersService.getByIdForSession(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("role", user.getRole().getValue());
        response.put("roles", user.getRoles().stream().map(r -> r.getValue()).toList());
        response.put("isEmailVerified", user.getIsEmailVerified());
        response.put("isOrgOwner", user.getIsOrgOwner());
        response.put("orgId", user.getOrganization() != null ? user.getOrganization().getId() : null);
        
        return response;
    }

    @PostMapping("/verify-email")
    public Map<String, Object> verifyEmail(@RequestBody @Valid VerifyEmailDto dto) {
        User user = authService.verifyEmail(dto.getToken());
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("isEmailVerified", user.getIsEmailVerified());
        
        return response;
    }

    @PostMapping("/password-strength")
    public Map<String, Object> passwordStrength(@RequestBody @Valid PasswordStrengthDto dto) {
        return authService.passwordStrength(dto.getPassword());
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody @Valid ForgotPasswordDto dto) {
        return authService.forgotPassword(dto.getEmail());
    }

    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestBody @Valid ResetPasswordDto dto) {
        User user = authService.resetPassword(dto.getToken(), dto.getNewPassword());
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("status", "ok");
        
        return response;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_ADMIN', 'SUPER_ADMIN', 'ORG_OWNER', 'PLATFORM_ADMIN')")
    public List<Map<String, Object>> listUsers(@RequestParam(required = false) String orgId) {
                List<User> users = authService.listUsers(orgId);
        
        return users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            map.put("username", user.getUsername());
            map.put("role", user.getRole().getValue());
            map.put("roles", user.getRoles().stream().map(r -> r.getValue()).toList());
            map.put("isEmailVerified", user.getIsEmailVerified());
            map.put("archivedAt", user.getArchivedAt());
            return map;
        }).toList();
    }
    
    // Helper methods
    
    private Long extractUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        if (auth.getPrincipal() instanceof String) {
            try {
                return Long.parseLong((String) auth.getPrincipal());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
