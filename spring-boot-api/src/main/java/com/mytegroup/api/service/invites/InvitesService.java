package com.mytegroup.api.service.invites;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.common.util.TokenHashUtil;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.core.InviteStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.InviteRepository;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.RoleExpansionHelper;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.email.EmailService;
import com.mytegroup.api.service.notifications.NotificationsService;
import com.mytegroup.api.service.persons.PersonsService;
import com.mytegroup.api.service.seats.SeatsService;
import com.mytegroup.api.service.users.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for invite management.
 * Handles creating, accepting, and managing user invites.
 * Mirrors NestJS invites.service.ts implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitesService {
    
    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final UsersService usersService;
    private final PersonsService personsService;
    private final SeatsService seatsService;
    private final EmailService emailService;
    private final NotificationsService notificationsService;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    
    @Value("${app.invites.default-expiry-hours:72}")
    private int defaultExpiryHours;
    
    @Value("${app.invites.throttle-window-minutes:10}")
    private int throttleWindowMinutes;
    
    @Value("${app.seats.default-per-org:5}")
    private int defaultSeatsPerOrg;
    
    /**
     * Creates a new invite for a person.
     */
    @Transactional
    public Invite create(Long personId, Role role, int expiresInHours, String orgId) {
        // Validate role assignment
        if (role == Role.SUPER_ADMIN) {
            throw new ForbiddenException("Cannot invite superadmin");
        }
        
        if (orgId == null || orgId.isEmpty()) {
            throw new BadRequestException("Missing organization context");
        }
        
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        // Expire stale invites first
        expireStaleInvites(orgIdLong);
        
        // Validate person exists and get their info
        Person person = personsService.getById(personId, orgId, false);
        
        String email = person.getPrimaryEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Person must have a primaryEmail to be invited");
        }
        email = email.toLowerCase().trim();
        
        // Validate person type for specific roles
        validatePersonTypeForRole(person, role);
        
        // Check if person already has a user
        if (person.getUser() != null) {
            throw new ConflictException("Person is already linked to a user; invite not required");
        }
        
        // Check if user already exists with this email
        User existingUser = usersService.findAnyByEmail(email);
        if (existingUser != null) {
            throw new ConflictException("A user already exists with this email");
        }
        
        // Check for existing pending invite
        Optional<Invite> existingInvite = inviteRepository.findPendingActiveInvite(
            orgIdLong, email, LocalDateTime.now());
        if (existingInvite.isPresent()) {
            throw new ConflictException("Pending invite already exists for this email");
        }
        
        // Throttle check
        LocalDateTime throttleSince = LocalDateTime.now().minusMinutes(throttleWindowMinutes);
        long recentCount = inviteRepository.countRecentInvites(orgIdLong, email, throttleSince);
        if (recentCount > 0) {
            throw new BadRequestException("Invite recently sent to this email; please wait before re-inviting");
        }
        
        // Generate token
        int expiryHours = expiresInHours > 0 ? expiresInHours : defaultExpiryHours;
        TokenHashUtil.TokenData tokenData = TokenHashUtil.generateTokenWithHashHours(expiryHours);
        
        // Create invite
        Invite invite = new Invite();
        invite.setOrganization(org);
        invite.setEmail(email);
        invite.setRole(role != null ? role : Role.USER);
        invite.setPerson(person);
        invite.setTokenHash(tokenData.hash());
        invite.setTokenExpires(tokenData.expires());
        invite.setStatus(InviteStatus.PENDING);
        // createdByUser will be set when sessions are implemented
        invite.setPiiStripped(false);
        invite.setLegalHold(false);
        
        Invite savedInvite = inviteRepository.save(invite);
        
        // Send invite email
        try {
            emailService.sendInviteEmail(email, tokenData.token(), orgId, org.getName(), null);
        } catch (Exception e) {
            log.error("Failed to send invite email: {}", e.getMessage());
            // Continue - invite is created even if email fails
        }
        
        // Audit log
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("email", email);
        metadata.put("role", role != null ? role.getValue() : Role.USER.getValue());
        metadata.put("personId", personId.toString());
        
        auditLogService.log(
            "invite.created",
            orgId,
            null,
            "Invite",
            savedInvite.getId().toString(),
            metadata
        );
        
        // Notification
        try {
            // TODO: Get userId from security context when sessions are implemented
            // notificationsService.create(orgId, userId, "invite.created", metadata);
        } catch (Exception e) {
            log.debug("Failed to create notification: {}", e.getMessage());
        }
        
        return savedInvite;
    }
    
    /**
     * Resends an invite with a new token.
     */
    @Transactional
    public Invite resend(Long inviteId, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        expireStaleInvites(orgIdLong);
        
        Invite invite = inviteRepository.findById(inviteId)
            .filter(i -> i.getOrganization().getId().equals(orgIdLong))
            .filter(i -> i.getArchivedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        
        if (invite.getStatus() == InviteStatus.ACCEPTED) {
            throw new BadRequestException("Invite already accepted");
        }
        
        // Generate new token
        TokenHashUtil.TokenData tokenData = TokenHashUtil.generateTokenWithHashHours(defaultExpiryHours);
        
        invite.setTokenHash(tokenData.hash());
        invite.setTokenExpires(tokenData.expires());
        invite.setStatus(InviteStatus.PENDING);
        
        Invite savedInvite = inviteRepository.save(invite);
        
        // Send invite email
        try {
            emailService.sendInviteEmail(invite.getEmail(), tokenData.token(), orgId, org.getName(), null);
        } catch (Exception e) {
            log.error("Failed to send invite email: {}", e.getMessage());
        }
        
        auditLogService.log(
            "invite.resent",
            orgId,
            null,
            "Invite",
            savedInvite.getId().toString(),
            null
        );
        
        return savedInvite;
    }
    
    /**
     * Accepts an invite and creates a user.
     */
    @Transactional
    public Map<String, Object> accept(String token, String username, String password) {
        // Expire any stale invites first
        inviteRepository.findByTokenExpiresBefore(LocalDateTime.now())
            .stream()
            .filter(i -> i.getStatus() == InviteStatus.PENDING)
            .forEach(i -> {
                i.setStatus(InviteStatus.EXPIRED);
                inviteRepository.save(i);
            });
        
        String tokenHash = TokenHashUtil.hashToken(token);
        
        Invite invite = inviteRepository.findByTokenHash(tokenHash)
            .filter(i -> i.getArchivedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("Invite not found or expired"));
        
        if (invite.getStatus() == InviteStatus.ACCEPTED) {
            throw new BadRequestException("Invite already accepted");
        }
        
        if (invite.getTokenExpires().isBefore(LocalDateTime.now())) {
            invite.setStatus(InviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new BadRequestException("Invite expired");
        }
        
        if (invite.getArchivedAt() != null) {
            throw new ForbiddenException("Invite unavailable");
        }
        
        String orgId = invite.getOrganization().getId().toString();
        
        // Ensure seats exist
        seatsService.ensureOrgSeats(orgId, defaultSeatsPerOrg);
        
        // Create user
        User user = new User();
        user.setUsername(username);
        user.setEmail(invite.getEmail());
        user.setPasswordHash(password); // Will be hashed in UsersService
        user.setOrganization(invite.getOrganization());
        user.setRole(invite.getRole() != null ? invite.getRole() : Role.USER);
        user.setRoles(new ArrayList<>(List.of(user.getRole())));
        user.setIsEmailVerified(true); // Verified by accepting invite
        user.setIsOrgOwner(false);
        
        User savedUser = usersService.create(user, true); // Enforce seat
        
        Long invitePersonId = invite.getPerson() != null ? invite.getPerson().getId() : null;
        Long invitedUserId = savedUser.getId();
        Long createdByUserId = invite.getCreatedByUser() != null ? invite.getCreatedByUser().getId() : null;
        
        // Update invite
        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        invite.setInvitedUser(savedUser);
        invite.setTokenHash(null); // Clear token after acceptance
        inviteRepository.save(invite);
        
        // Audit log
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("personId", invitePersonId != null ? invitePersonId.toString() : null);
        
        auditLogService.log(
            "invite.accepted",
            orgId,
            invitedUserId.toString(),
            "Invite",
            invite.getId().toString(),
            metadata
        );
        
        // Try to link person to user
        if (invitePersonId == null && invitedUserId != null) {
            try {
                // Try to find person by email
                Person person = personsService.findByPrimaryEmail(orgId, invite.getEmail());
                if (person != null) {
                    invitePersonId = person.getId();
                }
            } catch (Exception e) {
                log.debug("Could not find person by email: {}", e.getMessage());
            }
        }
        
        if (invitePersonId != null && invitedUserId != null) {
            try {
                personsService.linkUser(orgId, invitePersonId, invitedUserId);
            } catch (Exception e) {
                log.warn("Could not link person to user: {}", e.getMessage());
            }
        }
        
        // Send notification to invite creator
        if (createdByUserId != null) {
            try {
                Map<String, Object> notificationPayload = new HashMap<>();
                notificationPayload.put("email", invite.getEmail());
                notificationPayload.put("invitedUserId", invitedUserId.toString());
                notificationPayload.put("personId", invitePersonId != null ? invitePersonId.toString() : null);
                notificationPayload.put("role", invite.getRole() != null ? invite.getRole().getValue() : null);
                
                notificationsService.create(orgId, createdByUserId, "invite.accepted", notificationPayload);
            } catch (Exception e) {
                log.debug("Could not create notification: {}", e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("user", savedUser);
        return result;
    }
    
    /**
     * Lists invites for an organization.
     */
    @Transactional(readOnly = true)
    public List<Invite> list(String orgId, InviteStatus status) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        Long orgIdLong = Long.parseLong(orgId);
        
        expireStaleInvites(orgIdLong);
        
        if (status != null) {
            return inviteRepository.findByOrgIdAndStatus(orgIdLong, status);
        }
        return inviteRepository.findByOrgIdAndArchivedAtIsNull(orgIdLong);
    }
    
    /**
     * Cancels an invite.
     */
    @Transactional
    public Invite cancel(Long inviteId, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        Long orgIdLong = Long.parseLong(orgId);
        
        Invite invite = inviteRepository.findById(inviteId)
            .filter(i -> i.getOrganization().getId().equals(orgIdLong))
            .filter(i -> i.getArchivedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BadRequestException("Can only cancel pending invites");
        }
        
        invite.setStatus(InviteStatus.EXPIRED);
        invite.setArchivedAt(LocalDateTime.now());
        Invite savedInvite = inviteRepository.save(invite);
        
        auditLogService.log(
            "invite.cancelled",
            orgId,
            null,
            "Invite",
            savedInvite.getId().toString(),
            null
        );
        
        return savedInvite;
    }
    
    // ==================== Helper Methods ====================
    
    private void validateInviteRole(Role requestedRole) {
        if (requestedRole == Role.SUPER_ADMIN) {
            throw new ForbiddenException("Cannot invite superadmin");
        }
        
        // Role validation will be handled by Spring Security annotations on controllers
        // This method is kept for potential future use
    }
    
    private void validatePersonTypeForRole(Person person, Role role) {
        String personType = person.getPersonType() != null ? person.getPersonType().getValue() : null;
        
        if (role == Role.FOREMAN) {
            if (!"internal_union".equals(personType)) {
                throw new BadRequestException("Foreman invites must come from an internal_union (ironworker) Person");
            }
            if (person.getIronworkerNumber() == null || person.getIronworkerNumber().isEmpty()) {
                throw new BadRequestException("Foreman invites require ironworkerNumber on the Person");
            }
        } else if (role == Role.SUPERINTENDENT) {
            if (!List.of("internal_staff", "internal_union").contains(personType)) {
                throw new BadRequestException("Superintendent invites must come from internal_staff or internal_union Person");
            }
        } else {
            if (!"internal_staff".equals(personType)) {
                throw new BadRequestException("Invites must come from an internal_staff Person (except Foreman/Superintendent)");
            }
        }
    }
    
    private void expireStaleInvites(Long orgId) {
        List<Invite> stale = inviteRepository.findExpiredPendingInvites(orgId, LocalDateTime.now());
        for (Invite invite : stale) {
            invite.setStatus(InviteStatus.EXPIRED);
            inviteRepository.save(invite);
        }
    }
}
