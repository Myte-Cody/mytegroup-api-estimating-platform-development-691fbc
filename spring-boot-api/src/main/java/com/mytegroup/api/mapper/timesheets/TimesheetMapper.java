package com.mytegroup.api.mapper.timesheets;

import com.mytegroup.api.dto.response.TimesheetEntryResponseDto;
import com.mytegroup.api.dto.response.TimesheetResponseDto;
import com.mytegroup.api.dto.timesheets.CreateTimesheetDto;
import com.mytegroup.api.dto.timesheets.TimesheetEntryDto;
import com.mytegroup.api.dto.timesheets.UpdateTimesheetDto;
import com.mytegroup.api.entity.enums.timesheets.HoursType;
import com.mytegroup.api.entity.enums.timesheets.TimesheetStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.timesheets.Timesheet;
import com.mytegroup.api.entity.timesheets.TimesheetEntry;
import com.mytegroup.api.entity.core.Organization;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TimesheetMapper {

    public Timesheet toEntity(CreateTimesheetDto dto, Organization org, Project project, Person person) {
        Timesheet timesheet = new Timesheet();
        timesheet.setOrganization(org);
        timesheet.setProject(project);
        timesheet.setPerson(person);
        timesheet.setCrewId(dto.crewId());
        timesheet.setWorkDate(dto.date());
        timesheet.setCreatedBy(dto.createdBy());
        if (dto.entries() != null) {
            timesheet.setEntries(mapEntries(dto.entries(), timesheet));
        }
        return timesheet;
    }

    public void updateEntity(Timesheet timesheet, UpdateTimesheetDto dto) {
        if (dto.status() != null) {
            timesheet.setStatus(parseStatus(dto.status()));
        }
        if (dto.entries() != null) {
            timesheet.getEntries().clear();
            timesheet.getEntries().addAll(mapEntries(dto.entries(), timesheet));
        }
    }

    public TimesheetResponseDto toDto(Timesheet entity) {
        if (entity == null) {
            return null;
        }
        return TimesheetResponseDto.builder()
            .id(entity.getId())
            .orgId(entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null)
            .projectId(entity.getProject() != null ? entity.getProject().getId().toString() : null)
            .personId(entity.getPerson() != null ? entity.getPerson().getId().toString() : null)
            .crewId(entity.getCrewId())
            .date(entity.getWorkDate())
            .status(entity.getStatus() != null ? entity.getStatus().getValue() : null)
            .createdBy(entity.getCreatedBy())
            .submittedAt(entity.getSubmittedAt())
            .approvedBy(entity.getApprovedBy())
            .approvedAt(entity.getApprovedAt())
            .rejectedBy(entity.getRejectedBy())
            .rejectedAt(entity.getRejectedAt())
            .rejectionReason(entity.getRejectionReason())
            .entries(entity.getEntries() != null ? entity.getEntries().stream().map(this::toEntryDto).toList() : List.of())
            .archivedAt(entity.getArchivedAt())
            .piiStripped(entity.getPiiStripped())
            .legalHold(entity.getLegalHold())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private TimesheetEntryResponseDto toEntryDto(TimesheetEntry entry) {
        return TimesheetEntryResponseDto.builder()
            .id(entry.getId())
            .taskId(entry.getTaskId())
            .hours(entry.getHours())
            .hoursType(entry.getHoursType() != null ? entry.getHoursType().getValue() : null)
            .notes(entry.getNotes())
            .build();
    }

    private List<TimesheetEntry> mapEntries(List<TimesheetEntryDto> entries, Timesheet timesheet) {
        List<TimesheetEntry> mapped = new ArrayList<>();
        for (TimesheetEntryDto entryDto : entries) {
            TimesheetEntry entry = new TimesheetEntry();
            entry.setTimesheet(timesheet);
            entry.setTaskId(entryDto.taskId());
            entry.setHours(entryDto.hours());
            entry.setHoursType(parseHoursType(entryDto.hoursType()));
            entry.setNotes(entryDto.notes());
            mapped.add(entry);
        }
        return mapped;
    }

    private TimesheetStatus parseStatus(String status) {
        try {
            return TimesheetStatus.fromValue(status);
        } catch (IllegalArgumentException ex) {
            return TimesheetStatus.DRAFT;
        }
    }

    private HoursType parseHoursType(String hoursType) {
        if (hoursType == null) {
            return HoursType.REGULAR;
        }
        try {
            return HoursType.fromValue(hoursType);
        } catch (IllegalArgumentException ex) {
            return HoursType.REGULAR;
        }
    }
}
