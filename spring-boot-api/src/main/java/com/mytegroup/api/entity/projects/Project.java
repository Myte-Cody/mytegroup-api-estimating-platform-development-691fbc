package com.mytegroup.api.entity.projects;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.embeddable.CostCodeBudget;
import com.mytegroup.api.entity.projects.embeddable.ProjectBudget;
import com.mytegroup.api.entity.projects.embeddable.ProjectQuantities;
import com.mytegroup.api.entity.projects.embeddable.ProjectStaffing;
import com.mytegroup.api.entity.projects.embeddable.SeatAssignment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_project_org_name", columnList = "org_id, name"),
    @Index(name = "idx_project_org_code", columnList = "org_id, project_code"),
    @Index(name = "idx_project_org_archived", columnList = "org_id, archived_at")
})
@Audited
@Getter
@Setter
public class Project extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_code")
    private String projectCode;

    @Column(name = "status")
    private String status;

    @Column(name = "location")
    private String location;

    @Column(name = "bid_date")
    private LocalDate bidDate;

    @Column(name = "award_date")
    private LocalDate awardDate;

    @Column(name = "fabrication_start_date")
    private LocalDate fabricationStartDate;

    @Column(name = "fabrication_end_date")
    private LocalDate fabricationEndDate;

    @Column(name = "erection_start_date")
    private LocalDate erectionStartDate;

    @Column(name = "erection_end_date")
    private LocalDate erectionEndDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Embedded
    private ProjectBudget budget;

    @Embedded
    private ProjectQuantities quantities;

    @Embedded
    private ProjectStaffing staffing;

    @ElementCollection
    @CollectionTable(name = "project_cost_code_budgets", joinColumns = @JoinColumn(name = "project_id"))
    private List<CostCodeBudget> costCodeBudgets = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "project_seat_assignments", joinColumns = @JoinColumn(name = "project_id"))
    private List<SeatAssignment> seatAssignments = new ArrayList<>();

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

