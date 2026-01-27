package com.mytegroup.api.controller.crewassignments;

import com.mytegroup.api.dto.crewassignments.CreateCrewAssignmentDto;
import com.mytegroup.api.dto.crewassignments.UpdateCrewAssignmentDto;
import com.mytegroup.api.dto.response.CrewAssignmentResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.crew.CrewAssignment;
import com.mytegroup.api.entity.system.EventLog;
import com.mytegroup.api.mapper.crewassignments.CrewAssignmentMapper;
import com.mytegroup.api.repository.system.EventLogRepository;
import com.mytegroup.api.service.crew.CrewAssignmentsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/crew-assignments")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CrewAssignmentController {

    private final CrewAssignmentsService crewAssignmentsService;
    private final CrewAssignmentMapper crewAssignmentMapper;
    private final EventLogRepository eventLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'PM', 'FOREMAN', 'CREW_LEAD')")
    public PaginatedResponseDto<CrewAssignmentResponseDto> list(
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

        Page<CrewAssignment> assignments = crewAssignmentsService.list(
            orgId, projectId, personId, crewId, dateFrom, dateTo, status, includeArchived, page, limit);

        return PaginatedResponseDto.<CrewAssignmentResponseDto>builder()
            .data(assignments.getContent().stream().map(crewAssignmentMapper::toDto).toList())
            .total(assignments.getTotalElements())
            .page(page)
            .limit(limit)
            .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewAssignmentResponseDto create(@RequestBody @Valid CreateCrewAssignmentDto dto) {
        CrewAssignment assignment = crewAssignmentMapper.toEntity(dto, null, null, null);
        CrewAssignment saved = crewAssignmentsService.create(
            assignment, dto.orgId(), Long.parseLong(dto.projectId()), Long.parseLong(dto.personId()));
        return crewAssignmentMapper.toDto(saved);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewAssignmentResponseDto getById(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId,
                                             @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        return crewAssignmentMapper.toDto(crewAssignmentsService.getById(id, orgId, includeArchived));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewAssignmentResponseDto update(@PathVariable Long id,
                                            @RequestBody @Valid UpdateCrewAssignmentDto dto,
                                            @RequestParam(required = false) String orgId) {
        CrewAssignment updates = new CrewAssignment();
        crewAssignmentMapper.updateEntity(updates, dto);
        CrewAssignment saved = crewAssignmentsService.update(id, updates, orgId);
        return crewAssignmentMapper.toDto(saved);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewAssignmentResponseDto archive(@PathVariable Long id,
                                             @RequestParam(required = false) String orgId) {
        return crewAssignmentMapper.toDto(crewAssignmentsService.archive(id, orgId));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewAssignmentResponseDto unarchive(@PathVariable Long id,
                                               @RequestParam(required = false) String orgId) {
        return crewAssignmentMapper.toDto(crewAssignmentsService.unarchive(id, orgId));
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public PaginatedResponseDto<EventLog> events(@PathVariable Long id,
                                                 @RequestParam String orgId,
                                                 @RequestParam(required = false, defaultValue = "0") int page,
                                                 @RequestParam(required = false, defaultValue = "25") int limit) {
        Page<EventLog> events = eventLogRepository.findByOrganization_IdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Long.parseLong(orgId), "crew_assignment", id.toString(), PageRequest.of(page, limit));
        return PaginatedResponseDto.<EventLog>builder()
            .data(events.getContent())
            .total(events.getTotalElements())
            .page(page)
            .limit(limit)
            .build();
    }
}
