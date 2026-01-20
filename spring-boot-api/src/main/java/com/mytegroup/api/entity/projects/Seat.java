package com.mytegroup.api.entity.projects;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.projects.SeatStatus;
import com.mytegroup.api.entity.projects.embeddable.SeatHistoryEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_seat_org_seat_number", columnList = "org_id, seat_number", unique = true),
    @Index(name = "idx_seat_org_status", columnList = "org_id, status"),
    @Index(name = "idx_seat_org_role_status", columnList = "org_id, role, status"),
    @Index(name = "idx_seat_org_project", columnList = "org_id, project_id"),
    @Index(name = "idx_seat_org_user", columnList = "org_id, user_id", unique = true)
})
@Audited
@Getter
@Setter
public class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status = SeatStatus.VACANT;

    @Column(name = "role")
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @ElementCollection
    @CollectionTable(name = "seat_history", joinColumns = @JoinColumn(name = "seat_id"))
    private List<SeatHistoryEntry> history = new ArrayList<>();
}

