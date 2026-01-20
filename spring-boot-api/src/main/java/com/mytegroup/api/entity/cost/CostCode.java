package com.mytegroup.api.entity.cost;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "cost_codes", indexes = {
    @Index(name = "idx_cost_code_org_code", columnList = "org_id, code", unique = true),
    @Index(name = "idx_cost_code_org_category", columnList = "org_id, category"),
    @Index(name = "idx_cost_code_active", columnList = "active"),
    @Index(name = "idx_cost_code_import_job", columnList = "import_job_id")
})
@Audited
@Getter
@Setter
public class CostCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = false;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id")
    private CostCodeImportJob importJob;
}

