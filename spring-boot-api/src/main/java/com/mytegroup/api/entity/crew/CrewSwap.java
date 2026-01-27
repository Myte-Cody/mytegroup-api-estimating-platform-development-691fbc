package com.mytegroup.api.entity.crew;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.crew.CrewSwapStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_swaps", indexes = {
    @Index(name = "idx_crew_swap_org_project_person", columnList = "org_id, project_id, person_id"),
    @Index(name = "idx_crew_swap_org_status", columnList = "org_id, status"),
    @Index(name = "idx_crew_swap_org_created", columnList = "org_id, created_at"),
    @Index(name = "idx_crew_swap_org_archived", columnList = "org_id, archived_at")
})
@Audited
@Getter
@Setter
public class CrewSwap extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "from_crew_id", nullable = false)
    private String fromCrewId;

    @Column(name = "to_crew_id", nullable = false)
    private String toCrewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CrewSwapStatus status = CrewSwapStatus.REQUESTED;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "completed_by")
    private String completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}
