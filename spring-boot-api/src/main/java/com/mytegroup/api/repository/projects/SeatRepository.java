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
    Optional<Seat> findByOrganization_IdAndSeatNumber(Long organizationId, Integer seatNumber);

    // Find by status
    List<Seat> findByOrganization_IdAndStatus(Long organizationId, SeatStatus status);

    // Find by role and status
    List<Seat> findByOrganization_IdAndRoleAndStatus(Long organizationId, String role, SeatStatus status);

    // Find by project
    List<Seat> findByOrganization_IdAndProjectId(Long organizationId, Long projectId);

    // Find by user (unique when not null)
    Optional<Seat> findByOrganization_IdAndUserId(Long organizationId, Long userId);

    // List ordered by seat number
    List<Seat> findByOrganization_IdAndStatusOrderBySeatNumber(Long organizationId, SeatStatus status);

    // Find all for org
    List<Seat> findByOrganization_Id(Long organizationId);
}

