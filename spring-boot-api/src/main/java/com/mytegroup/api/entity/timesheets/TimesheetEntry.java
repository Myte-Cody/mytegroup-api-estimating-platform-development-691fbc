package com.mytegroup.api.entity.timesheets;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.enums.timesheets.HoursType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "timesheet_entries", indexes = {
    @Index(name = "idx_timesheet_entries_timesheet", columnList = "timesheet_id"),
    @Index(name = "idx_timesheet_entries_task", columnList = "task_id")
})
@Audited
@Getter
@Setter
public class TimesheetEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private Timesheet timesheet;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "hours", nullable = false)
    private Double hours;

    @Enumerated(EnumType.STRING)
    @Column(name = "hours_type", nullable = false)
    private HoursType hoursType = HoursType.REGULAR;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
