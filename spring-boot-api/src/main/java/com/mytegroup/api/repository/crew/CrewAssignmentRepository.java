package com.mytegroup.api.repository.crew;

import com.mytegroup.api.entity.crew.CrewAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CrewAssignmentRepository extends JpaRepository<CrewAssignment, Long>, JpaSpecificationExecutor<CrewAssignment> {

    List<CrewAssignment> findByOrganization_IdAndPerson_IdAndArchivedAtIsNull(Long orgId, Long personId);

    @Query("""
        SELECT c FROM CrewAssignment c
        WHERE c.organization.id = :orgId
          AND c.person.id = :personId
          AND c.archivedAt IS NULL
          AND (c.endDate IS NULL OR c.endDate >= :startDate)
          AND c.startDate <= :endDate
        """)
    List<CrewAssignment> findOverlappingAssignments(
        @Param("orgId") Long orgId,
        @Param("personId") Long personId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
