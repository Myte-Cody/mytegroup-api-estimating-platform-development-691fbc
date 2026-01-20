package com.mytegroup.api.entity.core;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_org_archived", columnList = "org_id, archived_at")
})
@Audited
@Getter
@Setter
public class User extends BaseEntity {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private List<Role> roles = new ArrayList<>(List.of(Role.USER));

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "verification_token_hash")
    private String verificationTokenHash;

    @Column(name = "verification_token_expires")
    private LocalDateTime verificationTokenExpires;

    @Column(name = "reset_token_hash")
    private String resetTokenHash;

    @Column(name = "reset_token_expires")
    private LocalDateTime resetTokenExpires;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_org_owner", nullable = false)
    private Boolean isOrgOwner = false;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

