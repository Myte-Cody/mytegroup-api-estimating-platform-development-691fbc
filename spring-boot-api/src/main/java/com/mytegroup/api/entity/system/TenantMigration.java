package com.mytegroup.api.entity.system;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.system.MigrationDirection;
import com.mytegroup.api.entity.enums.system.MigrationStatus;
import com.mytegroup.api.entity.system.embeddable.CollectionProgress;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "tenant_migrations", indexes = {
    @Index(name = "idx_tenant_migration_org", columnList = "org_id")
})
@Audited
@Getter
@Setter
public class TenantMigration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private MigrationDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MigrationStatus status = MigrationStatus.PENDING;

    @Column(name = "dry_run", nullable = false)
    private Boolean dryRun = false;

    @Column(name = "resume_requested", nullable = false)
    private Boolean resumeRequested = true;

    @Column(name = "allow_legal_hold_override", nullable = false)
    private Boolean allowLegalHoldOverride = false;

    @Column(name = "actor_user_id")
    private String actorUserId;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "target_uri")
    private String targetUri;

    @Column(name = "target_db_name")
    private String targetDbName;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize = 100;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "progress", columnDefinition = "jsonb")
    private Map<String, CollectionProgress> progress;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_progress_at")
    private LocalDateTime lastProgressAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}

