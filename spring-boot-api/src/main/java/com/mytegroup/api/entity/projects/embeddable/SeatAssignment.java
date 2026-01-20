package com.mytegroup.api.entity.projects.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatAssignment {

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "person_id")
    private Long personId;

    @Column(name = "role")
    private String role;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;
}

