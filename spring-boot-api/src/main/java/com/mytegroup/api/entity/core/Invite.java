package com.mytegroup.api.entity.core;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.enums.core.InviteStatus;
import com.mytegroup.api.entity.people.Person;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "invites", indexes = {
    @Index(name = "idx_invite_org_email_status", columnList = "org_id, email, status"),
    @Index(name = "idx_invite_org_person_status", columnList = "org_id, person_id, status"),
    @Index(name = "idx_invite_token_hash", columnList = "token_hash"),
    @Index(name = "idx_invite_token_expires", columnList = "token_expires")
})
@Audited
@Getter
@Setter
public class Invite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "token_expires", nullable = false)
    private LocalDateTime tokenExpires;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InviteStatus status = InviteStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user_id")
    private User invitedUser;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

