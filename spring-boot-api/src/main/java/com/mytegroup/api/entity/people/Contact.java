package com.mytegroup.api.entity.people;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.people.ContactKind;
import com.mytegroup.api.entity.enums.people.ContactPersonType;
import com.mytegroup.api.entity.people.embeddable.ContactCertification;
import com.mytegroup.api.entity.organization.Office;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contact_org_email", columnList = "org_id, email"),
    @Index(name = "idx_contact_org_person_type", columnList = "org_id, person_type, archived_at"),
    @Index(name = "idx_contact_org_ironworker", columnList = "org_id, ironworker_number"),
    @Index(name = "idx_contact_org_archived", columnList = "org_id, archived_at"),
    @Index(name = "idx_contact_org_location", columnList = "org_id, office_id"),
    @Index(name = "idx_contact_reports_to", columnList = "org_id, reports_to_contact_id")
})
@Audited
@Getter
@Setter
public class Contact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false)
    private ContactPersonType personType = ContactPersonType.EXTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_kind", nullable = false)
    private ContactKind contactKind = ContactKind.INDIVIDUAL;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "ironworker_number")
    private String ironworkerNumber;

    @Column(name = "union_local")
    private String unionLocal;

    @Column(name = "promoted_to_foreman", nullable = false)
    private Boolean promotedToForeman = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foreman_user_id")
    private User foremanUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_contact_id")
    private Contact reportsTo;

    @OneToMany(mappedBy = "reportsTo")
    private List<Contact> reports = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "contact_skills", joinColumns = @JoinColumn(name = "contact_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "contact_certifications", joinColumns = @JoinColumn(name = "contact_id"))
    private List<ContactCertification> certifications = new ArrayList<>();

    @Column(name = "rating")
    private Double rating;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "company")
    private String company;

    @ElementCollection
    @CollectionTable(name = "contact_roles", joinColumns = @JoinColumn(name = "contact_id"))
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "contact_tags", joinColumns = @JoinColumn(name = "contact_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user_id")
    private User invitedUser;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "invite_status")
    private String inviteStatus;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

