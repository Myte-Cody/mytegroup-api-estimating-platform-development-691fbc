package com.mytegroup.api.entity.projects;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.projects.EstimateStatus;
import com.mytegroup.api.entity.projects.embeddable.EstimateLineItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estimates", indexes = {
    @Index(name = "idx_estimate_org_project_name", columnList = "org_id, project_id, name"),
    @Index(name = "idx_estimate_org_project_archived", columnList = "org_id, project_id, archived_at")
})
@Audited
@Getter
@Setter
public class Estimate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EstimateStatus status = EstimateStatus.DRAFT;

    @Column(name = "total_amount")
    private Double totalAmount;

    @ElementCollection
    @CollectionTable(name = "estimate_line_items", joinColumns = @JoinColumn(name = "estimate_id"))
    private List<EstimateLineItem> lineItems = new ArrayList<>();

    @Column(name = "revision", nullable = false)
    private Integer revision = 1;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

