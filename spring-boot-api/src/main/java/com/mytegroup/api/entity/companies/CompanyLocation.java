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
@Table(name = "company_locations", indexes = {
    @Index(name = "idx_company_location_org_company_normalized", columnList = "org_id, company_id, normalized_name"),
    @Index(name = "idx_company_location_org_company_external", columnList = "org_id, company_id, external_id"),
    @Index(name = "idx_company_location_org_company", columnList = "org_id, company_id"),
    @Index(name = "idx_company_location_org_archived", columnList = "org_id, archived_at")
})
@Audited
@Getter
@Setter
public class CompanyLocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "region")
    private String region;

    @Column(name = "postal")
    private String postal;

    @Column(name = "country")
    private String country;

    @ElementCollection
    @CollectionTable(name = "company_location_tag_keys", joinColumns = @JoinColumn(name = "company_location_id"))
    @Column(name = "tag_key")
    private List<String> tagKeys = new ArrayList<>();

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

