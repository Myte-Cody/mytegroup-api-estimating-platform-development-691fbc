package com.mytegroup.api.controller.seats;

import com.mytegroup.api.entity.enums.projects.SeatStatus;
import com.mytegroup.api.entity.projects.Seat;
import com.mytegroup.api.service.common.ActorContext;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String status) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        SeatStatus seatStatus = status != null ? SeatStatus.valueOf(status.toUpperCase()) : null;
        
        List<Seat> seats = seatsService.list(resolvedOrgId, seatStatus);
        
        return seats.stream()
            .map(this::seatToMap)
            .toList();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> summary(@RequestParam(required = false) String orgId) {
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        SeatsService.SeatSummary summary = seatsService.summary(resolvedOrgId);
        
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
    public Map<String, Object> assignProject(
            @PathVariable Long seatId,
            @RequestParam String orgId,
            @RequestParam Long projectId,
            @RequestParam(required = false) String role) {
        
        Seat seat = seatsService.assignSeatToProject(orgId, seatId, projectId, role);
        
        return seatToMap(seat);
    }

    @PostMapping("/{seatId}/clear-project")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> clearProject(
            @PathVariable Long seatId,
            @RequestParam String orgId,
            @RequestParam Long projectId) {
        
        Seat seat = seatsService.clearSeatProject(orgId, seatId, projectId);
        
        return seatToMap(seat);
    }

    @PostMapping("/allocate")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> allocate(
            @RequestParam String orgId,
            @RequestParam Long userId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long projectId) {
        
        Seat seat = seatsService.allocateSeat(orgId, userId, role, projectId);
        
        return seatToMap(seat);
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
        
        return seatToMap(seat);
    }
    
    // Helper methods
    
    private Map<String, Object> seatToMap(Seat seat) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", seat.getId());
        map.put("seatNumber", seat.getSeatNumber());
        map.put("status", seat.getStatus() != null ? seat.getStatus().name() : null);
        map.put("role", seat.getRole());
        map.put("userId", seat.getUser() != null ? seat.getUser().getId() : null);
        map.put("projectId", seat.getProject() != null ? seat.getProject().getId() : null);
        map.put("activatedAt", seat.getActivatedAt());
        map.put("orgId", seat.getOrganization() != null ? seat.getOrganization().getId() : null);
        map.put("createdAt", seat.getCreatedAt());
        map.put("updatedAt", seat.getUpdatedAt());
        
        // Include history if present
        if (seat.getHistory() != null && !seat.getHistory().isEmpty()) {
            map.put("history", seat.getHistory().stream().map(h -> Map.of(
                "userId", h.getUserId(),
                "projectId", h.getProjectId(),
                "role", h.getRole(),
                "assignedAt", h.getAssignedAt(),
                "removedAt", h.getRemovedAt()
            )).toList());
        }
        
        return map;
    }
    
    private ActorContext getActorContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new ActorContext(null, null, null, null);
        }
        
        Long userId = null;
        if (auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        } else if (auth.getPrincipal() instanceof String) {
            try {
                userId = Long.parseLong((String) auth.getPrincipal());
            } catch (NumberFormatException ignored) {}
        }
        
        return new ActorContext(
            userId != null ? userId.toString() : null,
            null,
            null,
            null
        );
    }
}
