package com.mytegroup.api.repository.projects;

import com.mytegroup.api.entity.enums.projects.SeatStatus;
import com.mytegroup.api.entity.projects.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    // Find by seat number (unique)
    Optional<Seat> findByOrgIdAndSeatNumber(Long orgId, Integer seatNumber);

    // Find by status
    List<Seat> findByOrgIdAndStatus(Long orgId, SeatStatus status);

    // Find by role and status
    List<Seat> findByOrgIdAndRoleAndStatus(Long orgId, String role, SeatStatus status);

    // Find by project
    List<Seat> findByOrgIdAndProjectId(Long orgId, Long projectId);

    // Find by user (unique when not null)
    Optional<Seat> findByOrgIdAndUserId(Long orgId, Long userId);

    // List ordered by seat number
    List<Seat> findByOrgIdAndStatusOrderBySeatNumber(Long orgId, SeatStatus status);

    // Find all for org
    List<Seat> findByOrgId(Long orgId);
}

