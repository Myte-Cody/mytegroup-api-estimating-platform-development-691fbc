package com.mytegroup.api.repository.timesheets;

import com.mytegroup.api.entity.timesheets.TimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {
    List<TimesheetEntry> findByTimesheet_Id(Long timesheetId);
}
