package com.mytegroup.api.entity.legal;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.legal.LegalDocType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_acceptances", indexes = {
    @Index(name = "idx_legal_acceptance_user_doc_version", columnList = "user_id, doc_type, version", unique = true),
    @Index(name = "idx_legal_acceptance_org_doc_version", columnList = "org_id, doc_type, version")
})
@Audited
@Getter
@Setter
public class LegalAcceptance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private LegalDocType docType;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt = LocalDateTime.now();

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

