package com.mytegroup.api.entity.core;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import com.mytegroup.api.entity.enums.organization.DataResidency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_org_name", columnList = "name", unique = true),
    @Index(name = "idx_org_primary_domain", columnList = "primary_domain", unique = true)
})
@Audited
@Getter
@Setter
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "primary_domain", unique = true)
    private String primaryDomain;

    @Column(name = "use_dedicated_db", nullable = false)
    private Boolean useDedicatedDb = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "datastore_type", nullable = false)
    private DatastoreType datastoreType = DatastoreType.SHARED;

    @Column(name = "database_uri")
    private String databaseUri;

    @Column(name = "database_name")
    private String databaseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_residency", nullable = false)
    private DataResidency dataResidency = DataResidency.SHARED;

    @Column(name = "last_migrated_at")
    private LocalDateTime lastMigratedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datastore_history", columnDefinition = "jsonb")
    private List<Map<String, Object>> datastoreHistory = new ArrayList<>();

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<User> users = new ArrayList<>();
}

