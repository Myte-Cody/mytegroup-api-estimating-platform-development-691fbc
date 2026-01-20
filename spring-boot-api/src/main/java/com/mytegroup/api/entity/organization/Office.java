package com.mytegroup.api.entity.organization;

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
@Table(name = "offices", indexes = {
    @Index(name = "idx_office_org_normalized_name", columnList = "org_id, normalized_name"),
    @Index(name = "idx_office_org_archived", columnList = "org_id, archived_at"),
    @Index(name = "idx_office_org_parent", columnList = "org_id, parent_org_location_id")
})
@Audited
@Getter
@Setter
public class Office extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "org_location_type_key")
    private String orgLocationTypeKey;

    @ElementCollection
    @CollectionTable(name = "office_tag_keys", joinColumns = @JoinColumn(name = "office_id"))
    @Column(name = "tag_key")
    private List<String> tagKeys = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_org_location_id")
    private Office parent;

    @OneToMany(mappedBy = "parent")
    private List<Office> children = new ArrayList<>();

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "address")
    private String address;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

