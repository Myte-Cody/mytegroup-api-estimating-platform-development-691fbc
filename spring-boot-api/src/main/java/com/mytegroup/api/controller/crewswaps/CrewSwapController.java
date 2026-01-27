package com.mytegroup.api.controller.crewswaps;

import com.mytegroup.api.dto.crewswaps.*;
import com.mytegroup.api.dto.response.CrewSwapResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.crew.CrewSwap;
import com.mytegroup.api.entity.system.EventLog;
import com.mytegroup.api.mapper.crewswaps.CrewSwapMapper;
import com.mytegroup.api.repository.system.EventLogRepository;
import com.mytegroup.api.service.crew.CrewSwapsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crew-swaps")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CrewSwapController {

    private final CrewSwapsService crewSwapsService;
    private final CrewSwapMapper crewSwapMapper;
    private final EventLogRepository eventLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'PM', 'FOREMAN', 'CREW_LEAD')")
    public PaginatedResponseDto<CrewSwapResponseDto> list(
            @RequestParam String orgId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) String fromCrewId,
            @RequestParam(required = false) String toCrewId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {

        Page<CrewSwap> swaps = crewSwapsService.list(
            orgId, projectId, personId, fromCrewId, toCrewId, status, includeArchived, page, limit);

        return PaginatedResponseDto.<CrewSwapResponseDto>builder()
            .data(swaps.getContent().stream().map(crewSwapMapper::toDto).toList())
            .total(swaps.getTotalElements())
            .page(page)
            .limit(limit)
            .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'PM', 'FOREMAN', 'CREW_LEAD')")
    public CrewSwapResponseDto create(@RequestBody @Valid CreateCrewSwapDto dto) {
        CrewSwap swap = crewSwapMapper.toEntity(dto, null, null, null);
        CrewSwap saved = crewSwapsService.create(
            swap, dto.orgId(), Long.parseLong(dto.projectId()), Long.parseLong(dto.personId()));
        return crewSwapMapper.toDto(saved);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewSwapResponseDto getById(@PathVariable Long id,
                                       @RequestParam(required = false) String orgId,
                                       @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        return crewSwapMapper.toDto(crewSwapsService.getById(id, orgId, includeArchived));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewSwapResponseDto approve(@PathVariable Long id,
                                       @RequestParam(required = false) String orgId,
                                       @RequestBody @Valid ApproveCrewSwapDto dto) {
        return crewSwapMapper.toDto(crewSwapsService.approve(id, orgId, dto.approverId(), dto.comments()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewSwapResponseDto reject(@PathVariable Long id,
                                      @RequestParam(required = false) String orgId,
                                      @RequestBody @Valid RejectCrewSwapDto dto) {
        return crewSwapMapper.toDto(crewSwapsService.reject(id, orgId, dto.approverId(), dto.reason()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewSwapResponseDto complete(@PathVariable Long id,
                                        @RequestParam(required = false) String orgId,
                                        @RequestBody(required = false) CompleteCrewSwapDto dto) {
        String completedBy = dto != null ? dto.completedBy() : null;
        String comments = dto != null ? dto.comments() : null;
        return crewSwapMapper.toDto(crewSwapsService.complete(id, orgId, completedBy, comments));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'PM', 'FOREMAN', 'CREW_LEAD')")
    public CrewSwapResponseDto cancel(@PathVariable Long id,
                                      @RequestParam(required = false) String orgId,
                                      @RequestBody(required = false) CancelCrewSwapDto dto) {
        String canceledBy = dto != null ? dto.canceledBy() : null;
        String reason = dto != null ? dto.reason() : null;
        return crewSwapMapper.toDto(crewSwapsService.cancel(id, orgId, canceledBy, reason));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewSwapResponseDto archive(@PathVariable Long id,
                                       @RequestParam(required = false) String orgId) {
        return crewSwapMapper.toDto(crewSwapsService.archive(id, orgId));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public CrewSwapResponseDto unarchive(@PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        return crewSwapMapper.toDto(crewSwapsService.unarchive(id, orgId));
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public PaginatedResponseDto<EventLog> events(@PathVariable Long id,
                                                 @RequestParam String orgId,
                                                 @RequestParam(required = false, defaultValue = "0") int page,
                                                 @RequestParam(required = false, defaultValue = "25") int limit) {
        Page<EventLog> events = eventLogRepository.findByOrganization_IdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Long.parseLong(orgId), "crew_swap", id.toString(), PageRequest.of(page, limit));
        return PaginatedResponseDto.<EventLog>builder()
            .data(events.getContent())
            .total(events.getTotalElements())
            .page(page)
            .limit(limit)
            .build();
    }
}
