package com.mytegroup.api.entity.timesheets;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.timesheets.TimesheetStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timesheets", indexes = {
    @Index(name = "idx_timesheet_org_project_person_date", columnList = "org_id, project_id, person_id, work_date"),
    @Index(name = "idx_timesheet_org_status", columnList = "org_id, status"),
    @Index(name = "idx_timesheet_org_crew", columnList = "org_id, crew_id"),
    @Index(name = "idx_timesheet_org_archived", columnList = "org_id, archived_at")
})
@Audited
@Getter
@Setter
public class Timesheet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "crew_id")
    private String crewId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimesheetStatus status = TimesheetStatus.DRAFT;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

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

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetEntry> entries = new ArrayList<>();
}
