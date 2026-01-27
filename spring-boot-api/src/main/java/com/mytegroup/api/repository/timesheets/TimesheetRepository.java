package com.mytegroup.api.repository.timesheets;

import com.mytegroup.api.entity.timesheets.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long>, JpaSpecificationExecutor<Timesheet> {

    Page<Timesheet> findByOrganization_IdAndArchivedAtIsNull(Long organizationId, Pageable pageable);

    Optional<Timesheet> findByOrganization_IdAndProject_IdAndPerson_IdAndWorkDateAndArchivedAtIsNull(
        Long organizationId, Long projectId, Long personId, LocalDate workDate);
}
