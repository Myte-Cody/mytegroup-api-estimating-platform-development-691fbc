package com.mytegroup.api.service.auth;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.common.util.DomainUtil;
import com.mytegroup.api.common.util.PasswordValidator;
import com.mytegroup.api.common.util.TokenHashUtil;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.UnauthorizedException;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.email.EmailService;
import com.mytegroup.api.service.organizations.OrganizationsService;
import com.mytegroup.api.service.users.UsersService;
import com.mytegroup.api.service.waitlist.WaitlistService;
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

/**
 * Service for authentication.
 * Handles login, registration, email verification, and password reset.
 * Mirrors NestJS auth.service.ts implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UsersService usersService;
    private final OrganizationsService organizationsService;
    private final WaitlistService waitlistService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    
    @Value("${app.auth.verification-token-ttl-hours:24}")
    private int verificationTokenTtlHours;
    
    @Value("${app.auth.reset-token-ttl-hours:1}")
    private int resetTokenTtlHours;
    
    /**
     * Authenticates a user with email and password.
     */
    @Transactional(readOnly = true)
    public User login(String email, String password) {
        User user = usersService.findByEmail(email);
        boolean isValid = user != null && passwordEncoder.matches(password, user.getPasswordHash());
        
        String orgId = user != null && user.getOrganization() != null 
            ? user.getOrganization().getId().toString() 
            : null;
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("email", email);
        metadata.put("success", isValid);
        
        auditLogService.log(
            "auth.login",
            orgId,
            user != null ? user.getId().toString() : null,
            "User",
            user != null ? user.getId().toString() : null,
            metadata
        );
        
        if (!isValid) {
            throw new UnauthorizedException("Invalid credentials");
        }
        
        if (user.getArchivedAt() != null) {
            throw new ForbiddenException("User archived");
        }
        if (Boolean.TRUE.equals(user.getLegalHold())) {
            throw new ForbiddenException("User on legal hold");
        }
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new ForbiddenException("Email not verified");
        }
        
        usersService.markLastLogin(user.getId());
        
        return user;
    }
    
    /**
     * Registers a new user.
     */
    @Transactional
    public User register(String email, String password, String username, String firstName, 
                        String lastName, String organizationName, String orgId, 
                        boolean legalAccepted, String inviteToken) {
        
        // Validate legal acceptance
        if (!legalAccepted) {
            throw new BadRequestException("You must accept the Privacy Policy and Terms & Conditions to create an account");
        }
        
        // Validate password strength
        if (!PasswordValidator.isStrong(password)) {
            throw new BadRequestException(PasswordValidator.STRONG_PASSWORD_MESSAGE);
        }
        
        String normalizedEmail = DomainUtil.normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new BadRequestException("Invalid email");
        }
        
        String domain = DomainUtil.normalizeDomainFromEmail(normalizedEmail);
        if (domain == null || domain.isEmpty()) {
            throw new BadRequestException("Invalid email domain");
        }
        
        String derivedUsername = username != null && !username.trim().isEmpty()
            ? username.trim()
            : (firstName != null && lastName != null 
                ? (firstName.trim() + " " + lastName.trim()).trim() 
                : normalizedEmail.split("@")[0]);
        
        // ==================== Waitlist/Invite Gate Logic ====================
        
        Map<String, Object> waitlistEntry = null;
        try {
            waitlistEntry = waitlistService.findByEmail(normalizedEmail);
        } catch (Exception e) {
            log.debug("Waitlist lookup failed: {}", e.getMessage());
        }
        
        boolean inviteGateEnabled = waitlistService.shouldEnforceInviteGate();
        
        if (inviteGateEnabled) {
            String status = waitlistEntry != null ? (String) waitlistEntry.get("status") : "missing";
            
            if (waitlistEntry == null || !List.of("invited", "activated").contains(status)) {
                waitlistService.logEvent("waitlist.register_blocked", Map.of(
                    "email", normalizedEmail,
                    "status", status
                ));
                throw new ForbiddenException(
                    "Invite required. Request early access, verify your email + phone, and we will invite your company in a wave."
                );
            }
            
            String verifyStatus = (String) waitlistEntry.get("verifyStatus");
            String phoneVerifyStatus = (String) waitlistEntry.get("phoneVerifyStatus");
            boolean fullyVerified = "verified".equals(verifyStatus) && "verified".equals(phoneVerifyStatus);
            
            if (!fullyVerified) {
                waitlistService.logEvent("waitlist.register_blocked_unverified", Map.of(
                    "email", normalizedEmail,
                    "verifyStatus", verifyStatus != null ? verifyStatus : "null",
                    "phoneVerifyStatus", phoneVerifyStatus != null ? phoneVerifyStatus : "null"
                ));
                throw new ForbiddenException("Please verify your email and phone before creating your account.");
            }
            
            // Check invite token if required
            if (waitlistService.requiresInviteToken()) {
                if (inviteToken == null || inviteToken.isEmpty()) {
                    throw new ForbiddenException("Invite link required. Please use the invite email to register.");
                }
                
                String inviteHash = TokenHashUtil.hashToken(inviteToken);
                String storedHash = (String) waitlistEntry.get("inviteTokenHash");
                Object expiresAt = waitlistEntry.get("inviteTokenExpiresAt");
                
                boolean tokenExpired = false;
                if (expiresAt != null) {
                    LocalDateTime expires = expiresAt instanceof LocalDateTime 
                        ? (LocalDateTime) expiresAt 
                        : LocalDateTime.parse(expiresAt.toString());
                    tokenExpired = expires.isBefore(LocalDateTime.now());
                }
                
                if (storedHash == null || !storedHash.equals(inviteHash) || tokenExpired) {
                    throw new ForbiddenException("Invite link invalid or expired. Please request a fresh invite.");
                }
            }
        }
        
        // ==================== Domain Gate Logic ====================
        
        boolean domainLocked = false;
        if (waitlistService.domainGateEnabled()) {
            Organization existingDomainOrg = organizationsService.findByDomain(domain);
            if (existingDomainOrg != null) {
                waitlistService.logEvent("waitlist.register_blocked_domain", Map.of(
                    "email", normalizedEmail,
                    "domain", domain,
                    "orgId", existingDomainOrg.getId().toString()
                ));
                throw new ForbiddenException("Your company already has access. Please ask your org admin to invite you.");
            }
            
            boolean claimed = waitlistService.acquireDomainClaim(domain);
            domainLocked = claimed;
            if (!claimed) {
                waitlistService.logEvent("waitlist.register_blocked_domain_race", Map.of(
                    "email", normalizedEmail,
                    "domain", domain
                ));
                throw new ForbiddenException("Your company already has access. Please ask your org admin to invite you.");
            }
        }
        
        try {
            // ==================== Create Organization if needed ====================
            
            Organization org;
            Long createdOrgId = null;
            
            if (orgId == null || orgId.trim().isEmpty()) {
                Organization newOrg = new Organization();
                newOrg.setName(organizationName != null ? organizationName : derivedUsername + "'s Organization");
                newOrg.setPrimaryDomain(domain);
                org = organizationsService.create(newOrg);
                createdOrgId = org.getId();
            } else {
                org = organizationsService.findById(Long.parseLong(orgId));
            }
            
            // ==================== Create User ====================
            
            // Generate email verification token if invite gate is not enforced
            TokenHashUtil.TokenData emailVerification = inviteGateEnabled 
                ? null 
                : TokenHashUtil.generateTokenWithHashHours(verificationTokenTtlHours);
            
            Role assignedRole = createdOrgId != null ? Role.ORG_OWNER : Role.USER;
            
            User user = new User();
            user.setUsername(derivedUsername);
            user.setFirstName(firstName != null ? firstName.trim() : null);
            user.setLastName(lastName != null ? lastName.trim() : null);
            user.setEmail(normalizedEmail);
            user.setPasswordHash(password); // Will be hashed in UsersService.create
            user.setOrganization(org);
            user.setRole(assignedRole);
            user.setRoles(new ArrayList<>(List.of(assignedRole)));
            user.setIsOrgOwner(createdOrgId != null);
            user.setIsEmailVerified(inviteGateEnabled); // Auto-verified if invite gate is on
            user.setVerificationTokenHash(emailVerification != null ? emailVerification.hash() : null);
            user.setVerificationTokenExpires(emailVerification != null ? emailVerification.expires() : null);
            
            User savedUser = usersService.create(user, true); // Enforce seat
            
            // Set org owner if new org was created
            if (createdOrgId != null) {
                organizationsService.setOwner(createdOrgId, savedUser.getId());
            }
            
            // Mark activated in waitlist
            String cohortTag = waitlistEntry != null ? (String) waitlistEntry.get("cohortTag") : null;
            waitlistService.markActivated(normalizedEmail, cohortTag);
            
            // Audit log
            auditLogService.log(
                "auth.register",
                org.getId().toString(),
                savedUser.getId().toString(),
                "User",
                savedUser.getId().toString(),
                null
            );
            
            // Send verification email if needed
            if (emailVerification != null) {
                try {
                    emailService.sendVerificationEmail(
                        normalizedEmail, 
                        emailVerification.token(), 
                        org.getId().toString(), 
                        derivedUsername
                    );
                } catch (Exception e) {
                    log.error("Failed to send verification email: {}", e.getMessage());
                    // Don't fail registration if email fails
                }
            }
            
            return savedUser;
            
        } finally {
            // Always release domain claim on exit
            if (domainLocked) {
                waitlistService.releaseDomainClaim(domain);
            }
        }
    }
    
    /**
     * Verifies email with token.
     */
    @Transactional
    public User verifyEmail(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadRequestException("Token is required");
        }
        
        String hash = TokenHashUtil.hashToken(token);
        User user = usersService.findByVerificationToken(hash);
        
        if (user == null) {
            throw new BadRequestException("Invalid or expired token");
        }
        
        User updatedUser = usersService.clearVerificationToken(user.getId());
        
        String orgId = user.getOrganization() != null 
            ? user.getOrganization().getId().toString() 
            : null;
        
        auditLogService.log(
            "auth.verify_email",
            orgId,
            user.getId().toString(),
            "User",
            user.getId().toString(),
            null
        );
        
        return updatedUser;
    }
    
    /**
     * Initiates password reset.
     */
    @Transactional
    public Map<String, String> forgotPassword(String email) {
        User user = usersService.findByEmail(email);
        
        if (user == null) {
            // Don't leak existence
            return Map.of("status", "ok");
        }
        
        if (Boolean.TRUE.equals(user.getLegalHold()) || user.getArchivedAt() != null) {
            throw new ForbiddenException("User unavailable");
        }
        
        // Generate reset token
        TokenHashUtil.TokenData tokenData = TokenHashUtil.generateTokenWithHashHours(resetTokenTtlHours);
        
        usersService.setResetToken(user.getId(), tokenData.hash(), tokenData.expires());
        
        String orgId = user.getOrganization() != null 
            ? user.getOrganization().getId().toString() 
            : null;
        
        auditLogService.log(
            "auth.password_reset_requested",
            orgId,
            user.getId().toString(),
            "User",
            user.getId().toString(),
            null
        );
        
        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(
                user.getEmail(), 
                tokenData.token(), 
                orgId, 
                user.getUsername()
            );
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
            // Still return ok to not leak info
        }
        
        return Map.of("status", "ok");
    }
    
    /**
     * Resets password with token.
     */
    @Transactional
    public User resetPassword(String token, String newPassword) {
        // Validate password strength
        if (!PasswordValidator.isStrong(newPassword)) {
            throw new BadRequestException(PasswordValidator.STRONG_PASSWORD_MESSAGE);
        }
        
        if (token == null || token.isEmpty()) {
            throw new BadRequestException("Token is required");
        }
        
        String hash = TokenHashUtil.hashToken(token);
        User user = usersService.findByResetToken(hash);
        
        if (user == null) {
            throw new BadRequestException("Invalid or expired token");
        }
        
        if (Boolean.TRUE.equals(user.getLegalHold()) || user.getArchivedAt() != null) {
            throw new ForbiddenException("User unavailable");
        }
        
        User updatedUser = usersService.clearResetTokenAndSetPassword(user.getId(), newPassword);
        
        String orgId = user.getOrganization() != null 
            ? user.getOrganization().getId().toString() 
            : null;
        
        auditLogService.log(
            "auth.password_reset",
            orgId,
            user.getId().toString(),
            "User",
            user.getId().toString(),
            null
        );
        
        return updatedUser;
    }
    
    /**
     * Checks password strength and returns analysis.
     */
    public Map<String, Object> passwordStrength(String password) {
        PasswordValidator.PasswordStrengthResult result = PasswordValidator.getStrength(password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("score", result.score());
        response.put("crackTimesDisplay", result.crackTimesDisplay());
        response.put("feedback", result.feedback());
        response.put("guessesLog10", result.guessesLog10());
        
        return response;
    }
    
    /**
     * Lists users for an organization.
     */
    @Transactional(readOnly = true)
    public List<User> listUsers(String orgId) {
        if (orgId == null) {
            throw new com.mytegroup.api.exception.BadRequestException("orgId is required");
        }
        return usersService.list(orgId, false);
    }
}
