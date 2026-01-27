package com.mytegroup.api.controller.timesheets;

import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.dto.response.TimesheetResponseDto;
import com.mytegroup.api.dto.timesheets.*;
import com.mytegroup.api.entity.system.EventLog;
import com.mytegroup.api.entity.timesheets.Timesheet;
import com.mytegroup.api.mapper.timesheets.TimesheetMapper;
import com.mytegroup.api.repository.system.EventLogRepository;
import com.mytegroup.api.service.timesheets.TimesheetsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/timesheets")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetsService timesheetsService;
    private final TimesheetMapper timesheetMapper;
    private final EventLogRepository eventLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'PM', 'FOREMAN', 'CREW_LEAD')")
    public PaginatedResponseDto<TimesheetResponseDto> list(
            @RequestParam String orgId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) String crewId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {

        Page<Timesheet> timesheets = timesheetsService.list(
            orgId, projectId, personId, crewId, dateFrom, dateTo, status, includeArchived, page, limit);

        return PaginatedResponseDto.<TimesheetResponseDto>builder()
            .data(timesheets.getContent().stream().map(timesheetMapper::toDto).toList())
            .total(timesheets.getTotalElements())
            .page(page)
            .limit(limit)
            .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'PM', 'FOREMAN', 'CREW_LEAD')")
    public TimesheetResponseDto create(@RequestBody @Valid CreateTimesheetDto dto) {
        Timesheet timesheet = timesheetMapper.toEntity(dto, null, null, null);
        Timesheet saved = timesheetsService.create(
            timesheet, dto.orgId(), Long.parseLong(dto.projectId()), Long.parseLong(dto.personId()));
        return timesheetMapper.toDto(saved);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'PM', 'FOREMAN', 'CREW_LEAD')")
    public TimesheetResponseDto getById(@PathVariable Long id,
                                        @RequestParam(required = false) String orgId,
                                        @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        return timesheetMapper.toDto(timesheetsService.getById(id, orgId, includeArchived));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public TimesheetResponseDto update(@PathVariable Long id,
                                       @RequestBody @Valid UpdateTimesheetDto dto,
                                       @RequestParam(required = false) String orgId) {
        Timesheet updates = new Timesheet();
        timesheetMapper.updateEntity(updates, dto);
        Timesheet saved = timesheetsService.update(id, updates, orgId);
        return timesheetMapper.toDto(saved);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'PM', 'FOREMAN', 'CREW_LEAD')")
    public TimesheetResponseDto submit(@PathVariable Long id,
                                       @RequestParam(required = false) String orgId,
                                       @RequestBody(required = false) SubmitTimesheetDto dto) {
        String submittedBy = dto != null ? dto.submittedBy() : null;
        return timesheetMapper.toDto(timesheetsService.submit(id, orgId, submittedBy));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public TimesheetResponseDto approve(@PathVariable Long id,
                                        @RequestParam(required = false) String orgId,
                                        @RequestBody @Valid ApproveTimesheetDto dto) {
        return timesheetMapper.toDto(timesheetsService.approve(id, orgId, dto.approverId(), dto.comments()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public TimesheetResponseDto reject(@PathVariable Long id,
                                       @RequestParam(required = false) String orgId,
                                       @RequestBody @Valid RejectTimesheetDto dto) {
        return timesheetMapper.toDto(timesheetsService.reject(id, orgId, dto.approverId(), dto.reason()));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public TimesheetResponseDto archive(@PathVariable Long id,
                                        @RequestParam(required = false) String orgId) {
        return timesheetMapper.toDto(timesheetsService.archive(id, orgId));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public TimesheetResponseDto unarchive(@PathVariable Long id,
                                          @RequestParam(required = false) String orgId) {
        return timesheetMapper.toDto(timesheetsService.unarchive(id, orgId));
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public PaginatedResponseDto<EventLog> events(@PathVariable Long id,
                                                 @RequestParam String orgId,
                                                 @RequestParam(required = false, defaultValue = "0") int page,
                                                 @RequestParam(required = false, defaultValue = "25") int limit) {
        Page<EventLog> events = eventLogRepository.findByOrganization_IdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Long.parseLong(orgId), "timesheet", id.toString(), PageRequest.of(page, limit));
        return PaginatedResponseDto.<EventLog>builder()
            .data(events.getContent())
            .total(events.getTotalElements())
            .page(page)
            .limit(limit)
            .build();
    }
}
