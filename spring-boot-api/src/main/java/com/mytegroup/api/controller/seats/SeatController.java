package com.mytegroup.api.controller.seats;

import com.mytegroup.api.dto.response.SeatResponseDto;
import com.mytegroup.api.entity.enums.projects.SeatStatus;
import com.mytegroup.api.entity.projects.Seat;
import com.mytegroup.api.mapper.response.SeatResponseMapper;
import com.mytegroup.api.service.seats.SeatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seats controller.
 * Endpoints:
 * - GET /seats - List seats (Admin+)
 * - GET /seats/summary - Get seat summary (Admin+)
 * - POST /seats/ensure - Ensure org has seats (Admin+)
 * - POST /seats/:id/assign-project - Assign seat to project (Admin+)
 * - POST /seats/:id/clear-project - Clear project from seat (Admin+)
 */
@RestController
@RequestMapping("/api/seats")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SeatController {

    private final SeatsService seatsService;
    private final SeatResponseMapper seatResponseMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<SeatResponseDto> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String status) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        SeatStatus seatStatus = status != null ? SeatStatus.valueOf(status.toUpperCase()) : null;
        
        List<Seat> seats = seatsService.list(orgId, seatStatus);
        
        return seats.stream()
            .map(seatResponseMapper::toDto)
            .toList();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> summary(@RequestParam(required = false) String orgId) {
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        SeatsService.SeatSummary summary = seatsService.summary(orgId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("orgId", summary.orgId());
        response.put("total", summary.total());
        response.put("active", summary.active());
        response.put("vacant", summary.vacant());
        
        return response;
    }

    @PostMapping("/ensure")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> ensureSeats(
            @RequestParam String orgId,
            @RequestParam(defaultValue = "5") int totalSeats) {
        
        seatsService.ensureOrgSeats(orgId, totalSeats);
        
        return Map.of("status", "ok", "totalSeats", totalSeats);
    }

    @PostMapping("/{seatId}/assign-project")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public SeatResponseDto assignProject(
            @PathVariable Long seatId,
            @RequestParam String orgId,
            @RequestParam Long projectId,
            @RequestParam(required = false) String role) {
        
        Seat seat = seatsService.assignSeatToProject(orgId, seatId, projectId, role);
        
        return seatResponseMapper.toDto(seat);
    }

    @PostMapping("/{seatId}/clear-project")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public SeatResponseDto clearProject(
            @PathVariable Long seatId,
            @RequestParam String orgId,
            @RequestParam Long projectId) {
        
        Seat seat = seatsService.clearSeatProject(orgId, seatId, projectId);
        
        return seatResponseMapper.toDto(seat);
    }

    @PostMapping("/allocate")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public SeatResponseDto allocate(
            @RequestParam String orgId,
            @RequestParam Long userId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long projectId) {
        
        Seat seat = seatsService.allocateSeat(orgId, userId, role, projectId);
        
        return seatResponseMapper.toDto(seat);
    }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> release(
            @RequestParam String orgId,
            @RequestParam Long userId) {
        
        Seat seat = seatsService.releaseSeatForUser(orgId, userId);
        
        if (seat == null) {
            return Map.of("status", "no_seat_found");
        }
        
        return Map.of("status", "ok", "seat", seatResponseMapper.toDto(seat));
    }
    
}
