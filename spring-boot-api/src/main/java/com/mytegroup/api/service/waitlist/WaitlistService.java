package com.mytegroup.api.service.waitlist;

import com.mytegroup.api.common.util.DomainUtil;
import com.mytegroup.api.common.util.TokenHashUtil;
import com.mytegroup.api.entity.core.WaitlistEntry;
import com.mytegroup.api.entity.enums.core.WaitlistStatus;
import com.mytegroup.api.entity.enums.core.WaitlistVerifyStatus;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.WaitlistEntryRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.email.EmailService;
import com.mytegroup.api.service.users.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for waitlist management.
 * Handles waitlist entries, email/phone verification, and invite gating.
 * Mirrors NestJS waitlist.service.ts implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {
    
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final UsersService usersService;
    private final StringRedisTemplate redisTemplate;
    
    // Configuration values
    @Value("${app.waitlist.invite-gate-enabled:false}")
    private boolean inviteGateEnabled;
    
    @Value("${app.waitlist.domain-gate-enabled:false}")
    private boolean domainGateEnabled;
    
    @Value("${app.waitlist.require-invite-token:false}")
    private boolean requireInviteToken;
    
    @Value("${app.waitlist.verification.ttl-minutes:30}")
    private int verificationTtlMinutes;
    
    @Value("${app.waitlist.verification.code-length:6}")
    private int verificationCodeLength;
    
    @Value("${app.waitlist.verification.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${app.waitlist.verification.max-total-attempts:12}")
    private int maxTotalAttempts;
    
    @Value("${app.waitlist.verification.max-resends:6}")
    private int maxResends;
    
    @Value("${app.waitlist.verification.block-minutes:60}")
    private int blockMinutes;
    
    @Value("${app.waitlist.verification.resend-cooldown-minutes:2}")
    private int resendCooldownMinutes;
    
    @Value("${app.client.base-url:http://localhost:4001}")
    private String clientBaseUrl;
    
    // Personal email domains that are blocked
    private static final Set<String> DOMAIN_DENYLIST = DomainUtil.PERSONAL_EMAIL_DOMAINS;
    
    /**
     * Starts a waitlist entry (creates or updates).
     */
    @Transactional
    public Map<String, Object> start(String email, String phone, String name, String role, 
                                     String source, boolean preCreateAccount, boolean marketingConsent) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);
        
        if (!DomainUtil.isValidEmail(normalizedEmail)) {
            throw new BadRequestException("Invalid email");
        }
        
        if (!DomainUtil.isValidE164Phone(normalizedPhone)) {
            throw new BadRequestException("Invalid phone (expected E.164 format, e.g. +15145551234)");
        }
        
        // Check domain denylist
        String domain = DomainUtil.normalizeDomainFromEmail(normalizedEmail);
        if (domain != null && DOMAIN_DENYLIST.contains(domain)) {
            throw new ForbiddenException(
                "Please use your company email address (no personal providers like Gmail, Outlook, Yahoo, or iCloud)."
            );
        }
        
        // Check if user already exists
        if (hasActiveUser(normalizedEmail)) {
            logEvent("waitlist_skip_active", Map.of("email", normalizedEmail));
            return Map.of("status", "ok", "entry", null);
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find or create entry
        Optional<WaitlistEntry> existingOpt = waitlistEntryRepository.findByEmail(normalizedEmail);
        WaitlistEntry entry;
        
        if (existingOpt.isPresent()) {
            entry = existingOpt.get();
            
            // Check if blocked
            ensureNotBlocked(entry);
            ensurePhoneNotBlocked(entry);
            assertTotalLimits(entry);
        } else {
            entry = new WaitlistEntry();
            entry.setEmail(normalizedEmail);
            entry.setVerifyAttempts(0);
            entry.setVerifyAttemptTotal(0);
            entry.setVerifyResends(0);
            entry.setPhoneVerifyAttempts(0);
            entry.setPhoneVerifyAttemptTotal(0);
            entry.setPhoneVerifyResends(0);
            entry.setInviteFailureCount(0);
            entry.setPiiStripped(false);
            entry.setLegalHold(false);
        }
        
        // Check if already verified
        boolean emailAlreadyVerified = entry.getVerifyStatus() == WaitlistVerifyStatus.VERIFIED;
        boolean phoneAlreadyVerified = entry.getPhoneVerifyStatus() == WaitlistVerifyStatus.VERIFIED
            && normalizedPhone.equals(normalizePhone(entry.getPhone()));
        
        boolean needsEmailCode = !emailAlreadyVerified;
        boolean needsPhoneCode = !phoneAlreadyVerified;
        
        // Generate codes if needed
        String emailCode = needsEmailCode ? TokenHashUtil.generateNumericCode(verificationCodeLength) : null;
        String phoneCode = needsPhoneCode ? TokenHashUtil.generateNumericCode(verificationCodeLength) : null;
        LocalDateTime expires = (needsEmailCode || needsPhoneCode) 
            ? now.plusMinutes(verificationTtlMinutes) 
            : null;
        
        // Determine status
        WaitlistStatus status = entry.getStatus();
        if (status == null || status == WaitlistStatus.PENDING_COHORT) {
            status = WaitlistStatus.PENDING_COHORT;
        }
        
        // Update entry
        entry.setName(name);
        entry.setPhone(normalizedPhone);
        entry.setRole(role);
        entry.setSource(source != null ? source : "landing");
        entry.setPreCreateAccount(preCreateAccount);
        entry.setMarketingConsent(marketingConsent);
        entry.setStatus(status);
        
        if (emailAlreadyVerified) {
            entry.setVerifyStatus(WaitlistVerifyStatus.VERIFIED);
        } else {
            entry.setVerifyStatus(WaitlistVerifyStatus.UNVERIFIED);
            entry.setVerifyCode(emailCode);
            entry.setVerifyExpiresAt(expires);
            entry.setVerifyAttempts(0);
            entry.setLastVerifySentAt(now);
        }
        
        if (phoneAlreadyVerified) {
            entry.setPhoneVerifyStatus(WaitlistVerifyStatus.VERIFIED);
        } else {
            entry.setPhoneVerifyStatus(WaitlistVerifyStatus.UNVERIFIED);
            entry.setPhoneVerifyCode(phoneCode);
            entry.setPhoneVerifyExpiresAt(expires);
            entry.setPhoneVerifyAttempts(0);
            entry.setPhoneLastVerifySentAt(now);
        }
        
        WaitlistEntry savedEntry = waitlistEntryRepository.save(entry);
        
        // Audit log
        logEvent("waitlist_submit", Map.of(
            "email", normalizedEmail,
            "role", role != null ? role : "",
            "source", source != null ? source : "landing",
            "preCreateAccount", preCreateAccount,
            "marketingConsent", marketingConsent,
            "needsEmailVerification", needsEmailCode,
            "needsPhoneVerification", needsPhoneCode
        ));
        
        // Send verification emails/SMS
        if (emailCode != null) {
            try {
                emailService.sendWaitlistVerificationEmail(normalizedEmail, emailCode, name);
            } catch (Exception e) {
                log.error("Failed to send verification email: {}", e.getMessage());
            }
        }
        
        // TODO: Send phone verification SMS
        if (phoneCode != null) {
            log.info("Would send SMS verification to {}: {}", normalizedPhone, phoneCode);
        }
        
        // Determine response status
        String responseStatus;
        if (!needsEmailCode && !needsPhoneCode) {
            responseStatus = "verified";
        } else if (needsEmailCode && needsPhoneCode) {
            responseStatus = "verification_sent";
        } else if (needsEmailCode) {
            responseStatus = "email_verification_sent";
        } else {
            responseStatus = "phone_verification_sent";
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", responseStatus);
        result.put("entry", savedEntry);
        return result;
    }
    
    /**
     * Verifies email code.
     */
    @Transactional
    public WaitlistEntry verifyEmail(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        
        WaitlistEntry entry = waitlistEntryRepository.findByEmail(normalizedEmail)
            .filter(e -> e.getArchivedAt() == null)
            .orElseThrow(() -> new BadRequestException("Not on the waitlist yet"));
        
        ensureNotBlocked(entry);
        assertTotalLimits(entry);
        
        if (entry.getVerifyStatus() == WaitlistVerifyStatus.VERIFIED) {
            return entry;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check if code expired
        if (entry.getVerifyCode() == null || entry.getVerifyExpiresAt() == null 
            || entry.getVerifyExpiresAt().isBefore(now)) {
            logEvent("waitlist_verify_expired", Map.of("email", normalizedEmail));
            throw new BadRequestException("Verification code expired. Please request a new code.");
        }
        
        // Check attempts
        int attemptCount = entry.getVerifyAttempts() + 1;
        if (attemptCount > maxAttempts) {
            entry.setVerifyAttemptTotal(entry.getVerifyAttemptTotal() + 1);
            waitlistEntryRepository.save(entry);
            maybeBlock(entry, "max_attempts_per_code");
        }
        
        // Verify code
        if (!code.trim().equals(entry.getVerifyCode().trim())) {
            entry.setVerifyAttempts(attemptCount);
            entry.setVerifyAttemptTotal(entry.getVerifyAttemptTotal() + 1);
            WaitlistEntry saved = waitlistEntryRepository.save(entry);
            assertTotalLimits(saved);
            
            logEvent("waitlist_verify_invalid", Map.of(
                "email", normalizedEmail,
                "attemptCount", attemptCount
            ));
            throw new BadRequestException("Invalid verification code");
        }
        
        // Mark as verified
        entry.setVerifyStatus(WaitlistVerifyStatus.VERIFIED);
        entry.setVerifiedAt(now);
        entry.setVerifyCode(null);
        entry.setVerifyExpiresAt(null);
        entry.setVerifyAttempts(0);
        
        WaitlistEntry savedEntry = waitlistEntryRepository.save(entry);
        
        logEvent("waitlist_verified", Map.of("email", normalizedEmail));
        
        return savedEntry;
    }
    
    /**
     * Verifies phone code.
     */
    @Transactional
    public WaitlistEntry verifyPhone(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        
        WaitlistEntry entry = waitlistEntryRepository.findByEmail(normalizedEmail)
            .filter(e -> e.getArchivedAt() == null)
            .orElseThrow(() -> new BadRequestException("Not on the waitlist yet"));
        
        ensurePhoneNotBlocked(entry);
        assertTotalLimits(entry);
        
        if (entry.getPhoneVerifyStatus() == WaitlistVerifyStatus.VERIFIED) {
            return entry;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check if code expired
        if (entry.getPhoneVerifyCode() == null || entry.getPhoneVerifyExpiresAt() == null 
            || entry.getPhoneVerifyExpiresAt().isBefore(now)) {
            logEvent("waitlist_phone_verify_expired", Map.of("email", normalizedEmail));
            throw new BadRequestException("Verification code expired. Please request a new code.");
        }
        
        // Check attempts
        int attemptCount = entry.getPhoneVerifyAttempts() + 1;
        if (attemptCount > maxAttempts) {
            entry.setPhoneVerifyAttemptTotal(entry.getPhoneVerifyAttemptTotal() + 1);
            waitlistEntryRepository.save(entry);
            maybeBlockPhone(entry, "max_attempts_per_code");
        }
        
        // Verify code
        if (!code.trim().equals(entry.getPhoneVerifyCode().trim())) {
            entry.setPhoneVerifyAttempts(attemptCount);
            entry.setPhoneVerifyAttemptTotal(entry.getPhoneVerifyAttemptTotal() + 1);
            WaitlistEntry saved = waitlistEntryRepository.save(entry);
            assertTotalLimits(saved);
            
            logEvent("waitlist_phone_verify_invalid", Map.of(
                "email", normalizedEmail,
                "attemptCount", attemptCount
            ));
            throw new BadRequestException("Invalid verification code");
        }
        
        // Mark as verified
        entry.setPhoneVerifyStatus(WaitlistVerifyStatus.VERIFIED);
        entry.setPhoneVerifiedAt(now);
        entry.setPhoneVerifyCode(null);
        entry.setPhoneVerifyExpiresAt(null);
        entry.setPhoneVerifyAttempts(0);
        
        WaitlistEntry savedEntry = waitlistEntryRepository.save(entry);
        
        logEvent("waitlist_phone_verified", Map.of("email", normalizedEmail));
        
        return savedEntry;
    }
    
    /**
     * Resends email verification code.
     */
    @Transactional
    public Map<String, String> resendEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        
        WaitlistEntry entry = waitlistEntryRepository.findByEmail(normalizedEmail)
            .filter(e -> e.getArchivedAt() == null)
            .orElseThrow(() -> new BadRequestException("Not on the waitlist yet"));
        
        ensureNotBlocked(entry);
        assertTotalLimits(entry);
        
        if (entry.getVerifyStatus() == WaitlistVerifyStatus.VERIFIED) {
            return Map.of("status", "verified");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check cooldown
        if (entry.getLastVerifySentAt() != null 
            && now.isBefore(entry.getLastVerifySentAt().plusMinutes(resendCooldownMinutes))) {
            throw new ForbiddenException("Please wait a bit before requesting another code.");
        }
        
        // Generate new code
        String code = TokenHashUtil.generateNumericCode(verificationCodeLength);
        LocalDateTime expires = now.plusMinutes(verificationTtlMinutes);
        
        entry.setVerifyCode(code);
        entry.setVerifyExpiresAt(expires);
        entry.setVerifyAttempts(0);
        entry.setLastVerifySentAt(now);
        entry.setVerifyResends(entry.getVerifyResends() + 1);
        
        WaitlistEntry savedEntry = waitlistEntryRepository.save(entry);
        assertTotalLimits(savedEntry);
        
        logEvent("waitlist_verification_resent", Map.of("email", normalizedEmail));
        
        // Send email
        try {
            emailService.sendWaitlistVerificationEmail(normalizedEmail, code, entry.getName());
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }
        
        return Map.of("status", "sent");
    }
    
    /**
     * Finds a waitlist entry by email.
     * Returns as Map for compatibility with AuthService.
     * Returns null if entry not found (for existence checks).
     * @throws BadRequestException if email is invalid
     */
    @Transactional(readOnly = true)
    public Map<String, Object> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        
        return waitlistEntryRepository.findByEmail(normalizedEmail)
            .filter(e -> e.getArchivedAt() == null)
            .map(this::entryToMap)
            .orElse(null);
    }
    
    /**
     * Marks a waitlist entry as activated (used after registration).
     */
    @Transactional
    public void markActivated(String email, String cohortTag) {
        String normalizedEmail = normalizeEmail(email);
        
        waitlistEntryRepository.findByEmail(normalizedEmail)
            .filter(e -> e.getArchivedAt() == null)
            .ifPresent(entry -> {
                entry.setStatus(WaitlistStatus.ACTIVATED);
                entry.setActivatedAt(LocalDateTime.now());
                entry.setCohortTag(cohortTag);
                entry.setInviteTokenHash(null);
                entry.setInviteTokenExpiresAt(null);
                waitlistEntryRepository.save(entry);
            });
    }
    
    /**
     * Marks a waitlist entry as invited.
     */
    @Transactional
    public WaitlistEntry markInvited(String email, String cohortTag) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        
        WaitlistEntry entry = waitlistEntryRepository.findByEmail(normalizedEmail)
            .filter(e -> e.getArchivedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found"));
        
        assertFullyVerified(entry);
        
        // Generate invite token if required
        TokenHashUtil.TokenData tokenData = requireInviteToken 
            ? TokenHashUtil.generateTokenWithHashHours(24 * 7) 
            : null;
        
        // Send invite email
        try {
            String registerLink = buildRegisterLink(normalizedEmail, tokenData != null ? tokenData.token() : null);
            String domain = DomainUtil.normalizeDomainFromEmail(normalizedEmail);
            String calendly = "https://calendly.com/ahmed-mekallach/thought-exchange";
            emailService.sendWaitlistInviteEmail(normalizedEmail, registerLink, domain, calendly);
        } catch (Exception e) {
            entry.setInviteFailureCount(entry.getInviteFailureCount() + 1);
            waitlistEntryRepository.save(entry);
            throw e;
        }
        
        entry.setStatus(WaitlistStatus.INVITED);
        entry.setInvitedAt(LocalDateTime.now());
        entry.setCohortTag(cohortTag);
        entry.setVerifyCode(null);
        entry.setVerifyExpiresAt(null);
        entry.setPhoneVerifyCode(null);
        entry.setPhoneVerifyExpiresAt(null);
        entry.setInviteTokenHash(tokenData != null ? tokenData.hash() : null);
        entry.setInviteTokenExpiresAt(tokenData != null ? tokenData.expires() : null);
        
        WaitlistEntry savedEntry = waitlistEntryRepository.save(entry);
        
        logEvent("waitlist_invited", Map.of(
            "email", normalizedEmail,
            "cohortTag", cohortTag != null ? cohortTag : ""
        ));
        
        return savedEntry;
    }
    
    /**
     * Lists waitlist entries.
     */
    @Transactional(readOnly = true)
    public Page<WaitlistEntry> list(WaitlistStatus status, WaitlistVerifyStatus verifyStatus, 
                                     int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        
        if (status != null && verifyStatus != null) {
            return waitlistEntryRepository.findByVerifyStatusAndStatusOrderByCreatedAtAsc(
                verifyStatus, status, pageRequest);
        } else if (status != null) {
            return waitlistEntryRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest);
        }
        return waitlistEntryRepository.findAll(pageRequest);
    }
    
    /**
     * Gets waitlist statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long waitlistCount = waitlistEntryRepository.count();
        
        return Map.of(
            "waitlistCount", waitlistCount,
            "waitlistDisplayCount", waitlistCount, // Could apply marketing projection
            "freeSeatsPerOrg", 5
        );
    }
    
    /**
     * Logs a waitlist event.
     */
    public void logEvent(String event, Map<String, Object> meta) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("event", event);
        if (meta != null) {
            metadata.putAll(meta);
        }
        auditLogService.log("marketing.event", null, null, null, null, metadata);
    }
    
    /**
     * Checks if invite gate should be enforced.
     */
    public boolean shouldEnforceInviteGate() {
        return inviteGateEnabled;
    }
    
    /**
     * Checks if invite token is required.
     */
    public boolean requiresInviteToken() {
        return requireInviteToken;
    }
    
    /**
     * Checks if domain gate is enabled.
     */
    public boolean domainGateEnabled() {
        return domainGateEnabled;
    }
    
    /**
     * Acquires a domain claim (Redis lock).
     */
    public boolean acquireDomainClaim(String domain) {
        if (redisTemplate == null) {
            return true; // No Redis, allow
        }
        String key = "waitlist:domain:claim:" + domain;
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", 2, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(result);
    }
    
    /**
     * Releases a domain claim.
     */
    public void releaseDomainClaim(String domain) {
        if (redisTemplate == null) {
            return;
        }
        String key = "waitlist:domain:claim:" + domain;
        redisTemplate.delete(key);
    }
    
    // ==================== Helper Methods ====================
    
    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
    
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.trim();
    }
    
    private boolean hasActiveUser(String email) {
        try {
            return usersService.findByEmail(email) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void ensureNotBlocked(WaitlistEntry entry) {
        if (entry.getVerifyStatus() != WaitlistVerifyStatus.BLOCKED) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (entry.getVerifyBlockedUntil() != null && entry.getVerifyBlockedUntil().isAfter(now)) {
            throw new ForbiddenException("Too many attempts. Please try later.");
        }
        
        // Unblock
        entry.setVerifyStatus(WaitlistVerifyStatus.UNVERIFIED);
        entry.setVerifyBlockedAt(null);
        entry.setVerifyBlockedUntil(null);
        entry.setVerifyAttempts(0);
        entry.setVerifyResends(0);
        waitlistEntryRepository.save(entry);
    }
    
    private void ensurePhoneNotBlocked(WaitlistEntry entry) {
        if (entry.getPhoneVerifyStatus() != WaitlistVerifyStatus.BLOCKED) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (entry.getPhoneVerifyBlockedUntil() != null && entry.getPhoneVerifyBlockedUntil().isAfter(now)) {
            throw new ForbiddenException("Too many attempts. Please try later.");
        }
        
        // Unblock
        entry.setPhoneVerifyStatus(WaitlistVerifyStatus.UNVERIFIED);
        entry.setPhoneVerifyBlockedAt(null);
        entry.setPhoneVerifyBlockedUntil(null);
        entry.setPhoneVerifyAttempts(0);
        entry.setPhoneVerifyResends(0);
        waitlistEntryRepository.save(entry);
    }
    
    private void maybeBlock(WaitlistEntry entry, String reason) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusMinutes(blockMinutes);
        
        entry.setVerifyStatus(WaitlistVerifyStatus.BLOCKED);
        entry.setVerifyBlockedAt(now);
        entry.setVerifyBlockedUntil(until);
        entry.setVerifyCode(null);
        entry.setVerifyExpiresAt(null);
        waitlistEntryRepository.save(entry);
        
        logEvent("waitlist_blocked", Map.of("reason", reason, "email", entry.getEmail()));
        
        throw new ForbiddenException("Too many attempts. Please try later.");
    }
    
    private void maybeBlockPhone(WaitlistEntry entry, String reason) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusMinutes(blockMinutes);
        
        entry.setPhoneVerifyStatus(WaitlistVerifyStatus.BLOCKED);
        entry.setPhoneVerifyBlockedAt(now);
        entry.setPhoneVerifyBlockedUntil(until);
        entry.setPhoneVerifyCode(null);
        entry.setPhoneVerifyExpiresAt(null);
        waitlistEntryRepository.save(entry);
        
        logEvent("waitlist_blocked", Map.of("reason", "phone:" + reason, "email", entry.getEmail()));
        
        throw new ForbiddenException("Too many attempts. Please try later.");
    }
    
    private void assertTotalLimits(WaitlistEntry entry) {
        if (entry.getVerifyAttemptTotal() >= maxTotalAttempts) {
            maybeBlock(entry, "max_total_attempts");
        }
        if (entry.getVerifyResends() >= maxResends) {
            maybeBlock(entry, "max_total_resends");
        }
        if (entry.getPhoneVerifyAttemptTotal() >= maxTotalAttempts) {
            maybeBlockPhone(entry, "max_total_attempts");
        }
        if (entry.getPhoneVerifyResends() >= maxResends) {
            maybeBlockPhone(entry, "max_total_resends");
        }
    }
    
    private void assertFullyVerified(WaitlistEntry entry) {
        String phone = normalizePhone(entry.getPhone());
        if (!DomainUtil.isValidE164Phone(phone)) {
            throw new ForbiddenException("Valid phone required before an invite can be issued.");
        }
        if (entry.getVerifyStatus() != WaitlistVerifyStatus.VERIFIED 
            || entry.getPhoneVerifyStatus() != WaitlistVerifyStatus.VERIFIED) {
            throw new ForbiddenException("Please verify your email and phone to lock your spot.");
        }
    }
    
    private String buildRegisterLink(String email, String inviteToken) {
        StringBuilder sb = new StringBuilder(clientBaseUrl).append("/auth/register");
        boolean hasParam = false;
        
        if (email != null) {
            sb.append("?email=").append(urlEncode(email));
            hasParam = true;
        }
        if (inviteToken != null) {
            sb.append(hasParam ? "&" : "?").append("invite=").append(urlEncode(inviteToken));
        }
        
        return sb.toString();
    }
    
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }
    
    private Map<String, Object> entryToMap(WaitlistEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entry.getId());
        map.put("email", entry.getEmail());
        map.put("status", entry.getStatus() != null ? entry.getStatus().name().toLowerCase() : null);
        map.put("verifyStatus", entry.getVerifyStatus() != null ? entry.getVerifyStatus().name().toLowerCase() : null);
        map.put("phoneVerifyStatus", entry.getPhoneVerifyStatus() != null ? entry.getPhoneVerifyStatus().name().toLowerCase() : null);
        map.put("cohortTag", entry.getCohortTag());
        map.put("inviteTokenHash", entry.getInviteTokenHash());
        map.put("inviteTokenExpiresAt", entry.getInviteTokenExpiresAt());
        return map;
    }
}
