package com.mytegroup.api.entity.people;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.people.PersonType;
import com.mytegroup.api.entity.people.embeddable.PersonCertification;
import com.mytegroup.api.entity.people.embeddable.PersonEmail;
import com.mytegroup.api.entity.people.embeddable.PersonPhone;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
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
@Table(name = "persons", indexes = {
    @Index(name = "idx_person_org_archived", columnList = "org_id, archived_at"),
    @Index(name = "idx_person_org_primary_email", columnList = "org_id, primary_email"),
    @Index(name = "idx_person_org_external_id", columnList = "org_id, external_id"),
    @Index(name = "idx_person_org_primary_phone", columnList = "org_id, primary_phone_e164"),
    @Index(name = "idx_person_org_ironworker", columnList = "org_id, ironworker_number"),
    @Index(name = "idx_person_org_location", columnList = "org_id, org_location_id"),
    @Index(name = "idx_person_reports_to", columnList = "org_id, reports_to_person_id"),
    @Index(name = "idx_person_company", columnList = "org_id, company_id"),
    @Index(name = "idx_person_user", columnList = "org_id, user_id")
})
@Audited
@Getter
@Setter
public class Person extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "external_id")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false)
    private PersonType personType;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @ElementCollection
    @CollectionTable(name = "person_emails", joinColumns = @JoinColumn(name = "person_id"))
    private List<PersonEmail> emails = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "person_phones", joinColumns = @JoinColumn(name = "person_id"))
    private List<PersonPhone> phones = new ArrayList<>();

    @Column(name = "primary_email")
    private String primaryEmail;

    @Column(name = "primary_phone_e164")
    private String primaryPhoneE164;

    @ElementCollection
    @CollectionTable(name = "person_tag_keys", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "tag_key")
    private List<String> tagKeys = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "person_skill_keys", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "skill_key")
    private List<String> skillKeys = new ArrayList<>();

    @Column(name = "department_key")
    private String departmentKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_location_id")
    private Office orgLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_person_id")
    private Person reportsTo;

    @OneToMany(mappedBy = "reportsTo")
    private List<Person> reports = new ArrayList<>();

    @Column(name = "ironworker_number")
    private String ironworkerNumber;

    @Column(name = "union_local")
    private String unionLocal;

    @ElementCollection
    @CollectionTable(name = "person_skill_free_text", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "skill_text")
    private List<String> skillFreeText = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "person_certifications", joinColumns = @JoinColumn(name = "person_id"))
    private List<PersonCertification> certifications = new ArrayList<>();

    @Column(name = "rating")
    private Double rating;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_location_id")
    private CompanyLocation companyLocation;

    @Column(name = "title")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

