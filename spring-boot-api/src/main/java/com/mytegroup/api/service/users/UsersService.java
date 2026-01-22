package com.mytegroup.api.service.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.common.util.PasswordValidator;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.OrganizationRepository;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.RoleExpansionHelper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.seats.SeatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for user management.
 * Handles CRUD operations, role management, email verification, and password reset.
 * Mirrors NestJS users.service.ts implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsersService {
    
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final SeatsService seatsService;
    
    @Value("${app.seats.default-per-org:5}")
    private int defaultSeatsPerOrg;
    
    /**
     * Creates a new user with optional seat enforcement.
     */
    @Transactional
    public User create(User user, boolean enforceSeat) {
        String email = normalizeEmail(user.getEmail());
        if (email == null || email.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        
        // Check for email collision
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        
        // Validate organization exists
        if (user.getOrganization() == null || user.getOrganization().getId() == null) {
            throw new BadRequestException("Organization is required");
        }
        
        Long orgId = user.getOrganization().getId();
        
        // Ensure seats exist if enforcing
        if (enforceSeat) {
            seatsService.ensureOrgSeats(orgId.toString(), defaultSeatsPerOrg);
        }
        
        // Validate password strength if provided
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            if (!PasswordValidator.isStrong(user.getPasswordHash())) {
                throw new BadRequestException(PasswordValidator.STRONG_PASSWORD_MESSAGE);
            }
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        
        // Normalize email
        user.setEmail(email);
        
        // Normalize and merge roles
        List<Role> roles = normalizeRoles(user.getRole(), user.getRoles());
        Role primaryRole = resolvePrimaryRole(roles);
        
        // Role validation will be handled when sessions are implemented
        user.setRole(primaryRole);
        user.setRoles(roles);
        
        // Set defaults
        if (user.getIsEmailVerified() == null) {
            user.setIsEmailVerified(false);
        }
        if (user.getIsOrgOwner() == null) {
            user.setIsOrgOwner(roles.contains(Role.ORG_OWNER));
        }
        if (user.getPiiStripped() == null) {
            user.setPiiStripped(false);
        }
        if (user.getLegalHold() == null) {
            user.setLegalHold(false);
        }
        
        User savedUser = userRepository.save(user);
        
        // Allocate seat if enforcing
        if (enforceSeat) {
            seatsService.allocateSeat(orgId.toString(), savedUser.getId(), primaryRole.getValue(), null);
        }
        
        auditLogService.log(
            "user.created",
            orgId.toString(),
            null, // userId will be set when sessions are implemented
            "User",
            savedUser.getId().toString(),
            null
        );
        
        return savedUser;
    }
    
    /**
     * Creates a new user (no seat enforcement).
     */
    @Transactional
    public User create(User user) {
        return create(user, false);
    }
    
    /**
     * Finds a user by email (active only).
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        return userRepository.findByEmail(normalized)
            .filter(user -> user.getArchivedAt() == null)
            .orElse(null);
    }
    
    /**
     * Finds a user by email (including archived).
     */
    @Transactional(readOnly = true)
    public User findAnyByEmail(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        return userRepository.findByEmail(normalized).orElse(null);
    }
    
    /**
     * Lists users for an organization.
     */
    @Transactional(readOnly = true)
    public List<User> list(String orgId, boolean includeArchived) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        Long orgIdLong = Long.parseLong(orgId);
        
        if (includeArchived) {
            return userRepository.findByOrgId(orgIdLong);
        } else {
            return userRepository.findByOrgIdAndArchivedAtIsNull(orgIdLong, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        }
    }
    
    /**
     * Gets a user by ID.
     */
    @Transactional(readOnly = true)
    public User getById(Long id, boolean includeArchived) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (!includeArchived && user.getArchivedAt() != null) {
            throw new ResourceNotFoundException("User not found");
        }
        
        return user;
    }
    
    /**
     * Gets a user by ID (for session, no auth check).
     */
    @Transactional(readOnly = true)
    public User getByIdForSession(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    /**
     * Finds user by verification token hash.
     * @throws BadRequestException if token is invalid or expired
     */
    @Transactional(readOnly = true)
    public User findByVerificationToken(String hash) {
        return userRepository.findByVerificationTokenHash(hash, LocalDateTime.now())
            .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));
    }
    
    /**
     * Sets verification token for a user.
     */
    @Transactional
    public void setVerificationToken(Long userId, String hash, LocalDateTime expires) {
        userRepository.setVerificationToken(userId, hash, expires);
    }
    
    /**
     * Clears verification token and marks email as verified.
     */
    @Transactional
    public User clearVerificationToken(Long userId) {
        userRepository.clearVerificationToken(userId);
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    /**
     * Finds user by password reset token hash.
     * @throws BadRequestException if token is invalid or expired
     */
    @Transactional(readOnly = true)
    public User findByResetToken(String hash) {
        return userRepository.findByResetTokenHash(hash, LocalDateTime.now())
            .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
    }
    
    /**
     * Sets password reset token for a user.
     */
    @Transactional
    public void setResetToken(Long userId, String hash, LocalDateTime expires) {
        userRepository.setResetToken(userId, hash, expires);
    }
    
    /**
     * Clears reset token and sets new password.
     */
    @Transactional
    public User clearResetTokenAndSetPassword(Long userId, String newPassword) {
        if (!PasswordValidator.isStrong(newPassword)) {
            throw new BadRequestException(PasswordValidator.STRONG_PASSWORD_MESSAGE);
        }
        String passwordHash = passwordEncoder.encode(newPassword);
        userRepository.clearResetTokenAndSetPassword(userId, passwordHash);
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    /**
     * Marks last login timestamp.
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public User markLastLogin(Long userId) {
        userRepository.updateLastLogin(userId, LocalDateTime.now());
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    /**
     * Updates a user.
     */
    @Transactional
    public User update(Long id, User userUpdates) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getArchivedAt() != null) {
            throw new ForbiddenException("Cannot update archived users");
        }
        
        // Compliance checks will be handled when sessions are implemented
        boolean canManageCompliance = true; // TODO: Implement when sessions are added
        
        if (Boolean.TRUE.equals(user.getLegalHold()) && !canManageCompliance) {
            throw new ForbiddenException("User on legal hold");
        }
        
        Map<String, Object> changes = new HashMap<>();
        
        // Update email if provided
        if (userUpdates.getEmail() != null && !userUpdates.getEmail().equals(user.getEmail())) {
            String normalizedEmail = normalizeEmail(userUpdates.getEmail());
            if (userRepository.findByEmail(normalizedEmail)
                .filter(u -> !u.getId().equals(id))
                .isPresent()) {
                throw new ConflictException("Email already registered");
            }
            changes.put("email", Map.of("from", user.getEmail(), "to", normalizedEmail));
            user.setEmail(normalizedEmail);
        }
        
        // Update username if provided
        if (userUpdates.getUsername() != null) {
            changes.put("username", Map.of("from", user.getUsername(), "to", userUpdates.getUsername()));
            user.setUsername(userUpdates.getUsername());
        }
        
        // Update email verification status (requires compliance role)
        if (userUpdates.getIsEmailVerified() != null) {
            if (!canManageCompliance) {
                throw new ForbiddenException("Insufficient role to change verification state");
            }
            changes.put("isEmailVerified", Map.of("from", user.getIsEmailVerified(), "to", userUpdates.getIsEmailVerified()));
            user.setIsEmailVerified(userUpdates.getIsEmailVerified());
        }
        
        // Update compliance fields
        if (userUpdates.getPiiStripped() != null) {
            if (!canManageCompliance) {
                throw new ForbiddenException("Insufficient role to change compliance fields");
            }
            changes.put("piiStripped", Map.of("from", user.getPiiStripped(), "to", userUpdates.getPiiStripped()));
            user.setPiiStripped(userUpdates.getPiiStripped());
        }
        
        if (userUpdates.getLegalHold() != null) {
            if (!canManageCompliance) {
                throw new ForbiddenException("Insufficient role to change compliance fields");
            }
            changes.put("legalHold", Map.of("from", user.getLegalHold(), "to", userUpdates.getLegalHold()));
            user.setLegalHold(userUpdates.getLegalHold());
        }
        
        User savedUser = userRepository.save(user);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", changes);
        
        auditLogService.log(
            "user.updated",
            savedUser.getOrganization() != null ? savedUser.getOrganization().getId().toString() : null,
            null, // userId will be set when sessions are implemented
            "User",
            savedUser.getId().toString(),
            metadata
        );
        
        return savedUser;
    }
    
    /**
     * Updates user roles.
     */
    @Transactional
    public User updateRoles(Long userId, List<Role> newRoles) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getArchivedAt() != null) {
            throw new ForbiddenException("Cannot change roles for archived users");
        }
        
        if (Boolean.TRUE.equals(user.getLegalHold())) {
            throw new ForbiddenException("Cannot change roles while user is on legal hold");
        }
        
        List<Role> normalizedRoles = normalizeRoles(null, newRoles);
        // Role validation will be handled when sessions are implemented
        
        Role primaryRole = resolvePrimaryRole(normalizedRoles);
        
        user.setRoles(normalizedRoles);
        user.setRole(primaryRole);
        user.setIsOrgOwner(normalizedRoles.contains(Role.ORG_OWNER));
        
        User savedUser = userRepository.save(user);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("roles", normalizedRoles);
        
        auditLogService.log(
            "user.roles_updated",
            savedUser.getOrganization() != null ? savedUser.getOrganization().getId().toString() : null,
            null, // userId will be set when sessions are implemented
            "User",
            savedUser.getId().toString(),
            metadata
        );
        
        return savedUser;
    }
    
    /**
     * Archives a user.
     */
    @Transactional
    public User archive(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        authHelper.ensureNotOnLegalHold(user, "archive");
        
        if (user.getArchivedAt() != null) {
            return user;
        }
        
        user.setArchivedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Release seat
        if (user.getOrganization() != null) {
            seatsService.releaseSeatForUser(user.getOrganization().getId().toString(), savedUser.getId());
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedUser.getArchivedAt());
        
        auditLogService.log(
            "user.archived",
            savedUser.getOrganization() != null ? savedUser.getOrganization().getId().toString() : null,
            null, // userId will be set when sessions are implemented
            "User",
            savedUser.getId().toString(),
            metadata
        );
        
        return savedUser;
    }
    
    /**
     * Unarchives a user.
     */
    @Transactional
    public User unarchive(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        authHelper.ensureNotOnLegalHold(user, "unarchive");
        
        if (user.getArchivedAt() == null) {
            return user;
        }
        
        // Ensure seats and allocate
        if (user.getOrganization() != null) {
            String orgId = user.getOrganization().getId().toString();
            seatsService.ensureOrgSeats(orgId, defaultSeatsPerOrg);
            seatsService.allocateSeat(orgId, user.getId(), user.getRole().getValue(), null);
        }
        
        user.setArchivedAt(null);
        User savedUser = userRepository.save(user);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedUser.getArchivedAt());
        
        auditLogService.log(
            "user.unarchived",
            savedUser.getOrganization() != null ? savedUser.getOrganization().getId().toString() : null,
            null, // userId will be set when sessions are implemented
            "User",
            savedUser.getId().toString(),
            metadata
        );
        
        return savedUser;
    }
    
    /**
     * Gets user roles with expansion.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserRoles(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<Role> roles = normalizeRoles(user.getRole(), user.getRoles());
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("username", user.getUsername());
        result.put("roles", roles);
        result.put("role", user.getRole() != null ? user.getRole() : resolvePrimaryRole(roles));
        
        return result;
    }
    
    // ==================== Helper Methods ====================
    
    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
    
    private List<Role> normalizeRoles(Role primary, List<Role> roles) {
        List<Role> merged = new ArrayList<>();
        if (roles != null) {
            merged.addAll(roles);
        }
        if (primary != null && !merged.contains(primary)) {
            merged.add(primary);
        }
        // Remove nulls and duplicates
        merged = merged.stream()
            .distinct()
            .toList();
        
        if (merged.isEmpty()) {
            merged = new ArrayList<>(List.of(Role.USER));
        }
        return new ArrayList<>(merged);
    }
    
    private Role resolvePrimaryRole(List<Role> roles) {
        List<Role> priority = RoleExpansionHelper.getRolePriority();
        for (Role role : priority) {
            if (roles.contains(role)) {
                return role;
            }
        }
        return Role.USER;
    }
    
    // TODO: Re-implement validateRoleChange when sessions are implemented
    // private void validateRoleChange(List<Role> targetRoles) {
    //     // Role validation will be handled when sessions are implemented
    // }
    
    private boolean canManageCompliance(List<Role> actorRoles) {
        return actorRoles.stream().anyMatch(role ->
            role == Role.SUPER_ADMIN || role == Role.PLATFORM_ADMIN || role == Role.ADMIN ||
            role == Role.ORG_OWNER || role == Role.ORG_ADMIN || role == Role.COMPLIANCE ||
            role == Role.COMPLIANCE_OFFICER
        );
    }
}
