package com.mytegroup.api.service.seats;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.projects.SeatStatus;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.projects.Seat;
import com.mytegroup.api.entity.projects.embeddable.SeatHistoryEntry;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.repository.projects.SeatRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for seat management.
 * Handles seat allocation, deallocation, and organization seat provisioning.
 * Mirrors NestJS seats.service.ts implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatsService {
    
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    
    /**
     * Ensures an organization has the specified number of seats.
     * Creates any missing seats up to totalSeats.
     */
    @Transactional
    public void ensureOrgSeats(String orgId, int totalSeats) {
        int target = Math.max(0, totalSeats);
        if (orgId == null || target <= 0) {
            return;
        }
        
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        List<Seat> existing = seatRepository.findByOrgId(orgIdLong);
        Set<Integer> existingNumbers = existing.stream()
            .map(Seat::getSeatNumber)
            .collect(Collectors.toSet());
        
        List<Seat> toCreate = new ArrayList<>();
        for (int i = 1; i <= target; i++) {
            if (!existingNumbers.contains(i)) {
                Seat seat = new Seat();
                seat.setOrganization(org);
                seat.setSeatNumber(i);
                seat.setStatus(SeatStatus.VACANT);
                seat.setHistory(new ArrayList<>());
                toCreate.add(seat);
            }
        }
        
        if (toCreate.isEmpty()) {
            return;
        }
        
        try {
            seatRepository.saveAll(toCreate);
        } catch (Exception e) {
            // Duplicate key errors can happen if two requests seed simultaneously
            // Just log and continue - seats may already exist
            log.warn("Failed to seed seats for org {}: {}", orgId, e.getMessage());
            return;
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalSeats", target);
        metadata.put("created", toCreate.size());
        
        auditLogService.logMutation(
            "seeded",
            "seats",
            null,
            orgId,
            null,
            metadata,
            null
        );
    }
    
    /**
     * Allocates a vacant seat to a user.
     */
    @Transactional
    public Seat allocateSeat(String orgId, Long userId, String role, Long projectId) {
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        LocalDateTime now = LocalDateTime.now();
        String effectiveRole = role != null ? role : "user";
        
        // Check if user already has a seat
        Optional<Seat> existingSeat = seatRepository.findByOrgIdAndUserId(orgIdLong, userId);
        if (existingSeat.isPresent()) {
            throw new ForbiddenException("User already has an allocated seat");
        }
        
        // Find a vacant seat
        List<Seat> vacantSeats = seatRepository.findByOrgIdAndStatusOrderBySeatNumber(orgIdLong, SeatStatus.VACANT);
        if (vacantSeats.isEmpty()) {
            throw new ForbiddenException("No available seats for this organization");
        }
        
        Seat seat = vacantSeats.get(0);
        
        // Get user entity
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Get project entity if specified
        Project project = null;
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                .orElse(null);
        }
        
        // Update seat
        seat.setStatus(SeatStatus.ACTIVE);
        seat.setUser(user);
        seat.setRole(effectiveRole);
        seat.setProject(project);
        seat.setActivatedAt(now);
        
        // Add history entry
        SeatHistoryEntry historyEntry = new SeatHistoryEntry(
            userId,
            projectId,
            effectiveRole,
            now,
            null // removedAt is null for active assignment
        );
        if (seat.getHistory() == null) {
            seat.setHistory(new ArrayList<>());
        }
        seat.getHistory().add(historyEntry);
        
        Seat savedSeat = seatRepository.save(seat);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("seatNumber", savedSeat.getSeatNumber());
        metadata.put("role", effectiveRole);
        metadata.put("projectId", projectId != null ? projectId.toString() : null);
        
        auditLogService.logMutation(
            "allocated",
            "seat",
            savedSeat.getId().toString(),
            orgId,
            userId.toString(),
            metadata,
            null
        );
        
        return savedSeat;
    }
    
    /**
     * Releases a seat from a user, making it vacant.
     */
    @Transactional
    public Seat releaseSeatForUser(String orgId, Long userId) {
        if (orgId == null || userId == null) {
            return null;
        }
        
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        Optional<Seat> seatOpt = seatRepository.findByOrgIdAndUserId(orgIdLong, userId);
        if (seatOpt.isEmpty()) {
            return null; // No seat to release
        }
        
        Seat seat = seatOpt.get();
        LocalDateTime now = LocalDateTime.now();
        
        // Update history - mark last entry as removed
        if (seat.getHistory() != null && !seat.getHistory().isEmpty()) {
            // Find the last entry for this user that doesn't have a removedAt
            for (int i = seat.getHistory().size() - 1; i >= 0; i--) {
                SeatHistoryEntry entry = seat.getHistory().get(i);
                if (userId.equals(entry.getUserId()) && entry.getRemovedAt() == null) {
                    entry.setRemovedAt(now);
                    break;
                }
            }
        }
        
        // Clear seat
        seat.setStatus(SeatStatus.VACANT);
        seat.setUser(null);
        seat.setProject(null);
        seat.setRole(null);
        seat.setActivatedAt(null);
        
        Seat savedSeat = seatRepository.save(seat);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("seatNumber", savedSeat.getSeatNumber());
        
        auditLogService.logMutation(
            "released",
            "seat",
            savedSeat.getId().toString(),
            orgId,
            userId.toString(),
            metadata,
            null
        );
        
        return savedSeat;
    }
    
    /**
     * Assigns a seat to a project.
     */
    @Transactional
    public Seat assignSeatToProject(String orgId, Long seatId, Long projectId, String role) {
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        Seat seat = seatRepository.findById(seatId)
            .filter(s -> s.getOrganization().getId().equals(orgIdLong))
            .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
        
        if (seat.getStatus() != SeatStatus.ACTIVE || seat.getUser() == null) {
            throw new ForbiddenException("Seat must be active and assigned before linking to a project");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Update history - mark any previous project assignment as removed
        if (seat.getHistory() != null) {
            for (int i = seat.getHistory().size() - 1; i >= 0; i--) {
                SeatHistoryEntry entry = seat.getHistory().get(i);
                if (entry.getProjectId() != null && entry.getRemovedAt() == null
                    && !entry.getProjectId().equals(projectId)) {
                    entry.setRemovedAt(now);
                }
            }
        }
        
        // Get project entity
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        // Update seat
        seat.setProject(project);
        if (role != null) {
            seat.setRole(role);
        }
        
        // Add history entry
        SeatHistoryEntry historyEntry = new SeatHistoryEntry(
            seat.getUser().getId(),
            projectId,
            seat.getRole(),
            now,
            null
        );
        if (seat.getHistory() == null) {
            seat.setHistory(new ArrayList<>());
        }
        seat.getHistory().add(historyEntry);
        
        Seat savedSeat = seatRepository.save(seat);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("seatNumber", savedSeat.getSeatNumber());
        metadata.put("projectId", projectId.toString());
        metadata.put("role", seat.getRole());
        
        auditLogService.logMutation(
            "project_assigned",
            "seat",
            savedSeat.getId().toString(),
            orgId,
            seat.getUser().getId().toString(),
            metadata,
            null
        );
        
        return savedSeat;
    }
    
    /**
     * Clears a project assignment from a seat.
     */
    @Transactional
    public Seat clearSeatProject(String orgId, Long seatId, Long projectId) {
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        Seat seat = seatRepository.findById(seatId)
            .filter(s -> s.getOrganization().getId().equals(orgIdLong))
            .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
        
        LocalDateTime now = LocalDateTime.now();
        
        // Update history - mark project assignment as removed
        if (seat.getHistory() != null) {
            for (int i = seat.getHistory().size() - 1; i >= 0; i--) {
                SeatHistoryEntry entry = seat.getHistory().get(i);
                if (projectId.equals(entry.getProjectId()) && entry.getRemovedAt() == null) {
                    entry.setRemovedAt(now);
                    break;
                }
            }
        }
        
        // Clear project if it matches
        if (seat.getProject() != null && seat.getProject().getId().equals(projectId)) {
            seat.setProject(null);
        }
        
        Seat savedSeat = seatRepository.save(seat);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("seatNumber", savedSeat.getSeatNumber());
        metadata.put("projectId", projectId.toString());
        
        auditLogService.logMutation(
            "project_unassigned",
            "seat",
            savedSeat.getId().toString(),
            orgId,
            seat.getUser() != null ? seat.getUser().getId().toString() : null,
            metadata,
            null
        );
        
        return savedSeat;
    }
    
    /**
     * Gets seat summary for an organization.
     */
    @Transactional(readOnly = true)
    public SeatSummary summary(String orgId) {
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        List<Seat> allSeats = seatRepository.findByOrgId(orgIdLong);
        int total = allSeats.size();
        int active = (int) allSeats.stream()
            .filter(s -> s.getStatus() == SeatStatus.ACTIVE)
            .count();
        int vacant = total - active;
        
        return new SeatSummary(orgId, total, active, vacant);
    }
    
    /**
     * Lists seats for an organization.
     */
    @Transactional(readOnly = true)
    public List<Seat> list(String orgId, SeatStatus status) {
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        if (status != null) {
            return seatRepository.findByOrgIdAndStatusOrderBySeatNumber(orgIdLong, status);
        }
        return seatRepository.findByOrgId(orgIdLong);
    }
    
    /**
     * Finds an active seat for a user.
     */
    @Transactional(readOnly = true)
    public Optional<Seat> findActiveSeatForUser(String orgId, Long userId) {
        Organization org = authHelper.validateOrg(orgId);
        Long orgIdLong = org.getId();
        
        return seatRepository.findByOrgIdAndUserId(orgIdLong, userId)
            .filter(seat -> seat.getStatus() == SeatStatus.ACTIVE);
    }
    
    /**
     * Summary record for seat statistics.
     */
    public record SeatSummary(String orgId, int total, int active, int vacant) {}
}
