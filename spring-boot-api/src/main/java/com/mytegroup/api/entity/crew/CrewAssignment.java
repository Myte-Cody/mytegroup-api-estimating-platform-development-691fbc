package com.mytegroup.api.entity.crew;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.crew.CrewAssignmentStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crew_assignments", indexes = {
    @Index(name = "idx_crew_assign_org_project_person", columnList = "org_id, project_id, person_id"),
    @Index(name = "idx_crew_assign_org_crew", columnList = "org_id, crew_id"),
    @Index(name = "idx_crew_assign_org_status", columnList = "org_id, status"),
    @Index(name = "idx_crew_assign_org_dates", columnList = "org_id, start_date, end_date"),
    @Index(name = "idx_crew_assign_org_archived", columnList = "org_id, archived_at")
})
@Audited
@Getter
@Setter
public class CrewAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "crew_id", nullable = false)
    private String crewId;

    @Column(name = "role_key")
    private String roleKey;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CrewAssignmentStatus status = CrewAssignmentStatus.ACTIVE;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}
