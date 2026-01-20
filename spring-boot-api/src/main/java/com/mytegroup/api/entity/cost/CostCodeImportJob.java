package com.mytegroup.api.entity.cost;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.cost.embeddable.CostCodeImportPreview;
import com.mytegroup.api.entity.enums.cost.CostCodeImportStatus;
import com.mytegroup.api.entity.system.embeddable.CollectionProgress;
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
@Table(name = "cost_code_import_jobs", indexes = {
    @Index(name = "idx_cost_code_import_job_org", columnList = "org_id")
})
@Audited
@Getter
@Setter
public class CostCodeImportJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CostCodeImportStatus status = CostCodeImportStatus.QUEUED;

    @ElementCollection
    @CollectionTable(name = "cost_code_import_previews", joinColumns = @JoinColumn(name = "cost_code_import_job_id"))
    private List<CostCodeImportPreview> preview = new ArrayList<>();

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_mime")
    private String fileMime;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_base64", columnDefinition = "TEXT")
    private String fileBase64;

    @Column(name = "dry_run", nullable = false)
    private Boolean dryRun = false;

    @Column(name = "resume_requested", nullable = false)
    private Boolean resumeRequested = true;

    @Column(name = "allow_legal_hold_override", nullable = false)
    private Boolean allowLegalHoldOverride = false;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize = 100;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "progress", columnDefinition = "jsonb")
    private Map<String, CollectionProgress> progress;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "importJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CostCode> costCodes = new ArrayList<>();
}

