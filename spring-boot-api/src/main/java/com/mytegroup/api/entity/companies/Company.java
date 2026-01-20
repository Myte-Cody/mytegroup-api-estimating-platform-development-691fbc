package com.mytegroup.api.entity.companies;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies", indexes = {
    @Index(name = "idx_company_org_normalized_name", columnList = "org_id, normalized_name"),
    @Index(name = "idx_company_org_external_id", columnList = "org_id, external_id"),
    @Index(name = "idx_company_org_archived", columnList = "org_id, archived_at"),
    @Index(name = "idx_company_org_type_keys", columnList = "org_id, company_type_keys"),
    @Index(name = "idx_company_org_tag_keys", columnList = "org_id, tag_keys")
})
@Audited
@Getter
@Setter
public class Company extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "website")
    private String website;

    @Column(name = "main_email")
    private String mainEmail;

    @Column(name = "main_phone")
    private String mainPhone;

    @ElementCollection
    @CollectionTable(name = "company_type_keys", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "type_key")
    private List<String> companyTypeKeys = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "company_tag_keys", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "tag_key")
    private List<String> tagKeys = new ArrayList<>();

    @Column(name = "rating")
    private Double rating;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyLocation> locations = new ArrayList<>();
}

