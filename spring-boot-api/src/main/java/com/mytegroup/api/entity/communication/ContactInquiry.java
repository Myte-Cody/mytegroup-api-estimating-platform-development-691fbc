package com.mytegroup.api.entity.communication;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_inquiries", indexes = {
    @Index(name = "idx_contact_inquiry_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_contact_inquiry_status_created", columnList = "status, created_at DESC")
})
@Audited
@Getter
@Setter
public class ContactInquiry extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "source")
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContactInquiryStatus status = ContactInquiryStatus.NEW;

    @Column(name = "ip")
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "responded_by")
    private String respondedBy;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

