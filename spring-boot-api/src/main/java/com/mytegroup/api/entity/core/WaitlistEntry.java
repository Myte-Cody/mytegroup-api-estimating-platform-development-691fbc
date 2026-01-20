package com.mytegroup.api.entity.core;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.enums.core.WaitlistStatus;
import com.mytegroup.api.entity.enums.core.WaitlistVerifyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "waitlist_entries", indexes = {
    @Index(name = "idx_waitlist_email", columnList = "email", unique = true),
    @Index(name = "idx_waitlist_status_created", columnList = "status, created_at DESC"),
    @Index(name = "idx_waitlist_verify_status", columnList = "verify_status, status, created_at")
})
@Audited
@Getter
@Setter
public class WaitlistEntry extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "source")
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WaitlistStatus status = WaitlistStatus.PENDING_COHORT;

    @Enumerated(EnumType.STRING)
    @Column(name = "verify_status", nullable = false)
    private WaitlistVerifyStatus verifyStatus = WaitlistVerifyStatus.UNVERIFIED;

    @Column(name = "verify_code")
    private String verifyCode;

    @Column(name = "verify_expires_at")
    private LocalDateTime verifyExpiresAt;

    @Column(name = "verify_attempts", nullable = false)
    private Integer verifyAttempts = 0;

    @Column(name = "verify_attempt_total", nullable = false)
    private Integer verifyAttemptTotal = 0;

    @Column(name = "verify_resends", nullable = false)
    private Integer verifyResends = 0;

    @Column(name = "last_verify_sent_at")
    private LocalDateTime lastVerifySentAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verify_blocked_at")
    private LocalDateTime verifyBlockedAt;

    @Column(name = "verify_blocked_until")
    private LocalDateTime verifyBlockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "phone_verify_status", nullable = false)
    private WaitlistVerifyStatus phoneVerifyStatus = WaitlistVerifyStatus.UNVERIFIED;

    @Column(name = "phone_verify_code")
    private String phoneVerifyCode;

    @Column(name = "phone_verify_expires_at")
    private LocalDateTime phoneVerifyExpiresAt;

    @Column(name = "phone_verify_attempts", nullable = false)
    private Integer phoneVerifyAttempts = 0;

    @Column(name = "phone_verify_attempt_total", nullable = false)
    private Integer phoneVerifyAttemptTotal = 0;

    @Column(name = "phone_verify_resends", nullable = false)
    private Integer phoneVerifyResends = 0;

    @Column(name = "phone_last_verify_sent_at")
    private LocalDateTime phoneLastVerifySentAt;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "phone_verify_blocked_at")
    private LocalDateTime phoneVerifyBlockedAt;

    @Column(name = "phone_verify_blocked_until")
    private LocalDateTime phoneVerifyBlockedUntil;

    @Column(name = "pre_create_account", nullable = false)
    private Boolean preCreateAccount = false;

    @Column(name = "marketing_consent", nullable = false)
    private Boolean marketingConsent = false;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "cohort_tag")
    private String cohortTag;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "invite_failure_count")
    private Integer inviteFailureCount = 0;

    @Column(name = "invite_token_hash")
    private String inviteTokenHash;

    @Column(name = "invite_token_expires_at")
    private LocalDateTime inviteTokenExpiresAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

