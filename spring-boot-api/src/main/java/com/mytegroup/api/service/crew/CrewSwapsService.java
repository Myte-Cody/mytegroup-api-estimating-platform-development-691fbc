package com.mytegroup.api.service.crew;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.crew.CrewSwap;
import com.mytegroup.api.entity.enums.crew.CrewSwapStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.crew.CrewSwapRepository;
import com.mytegroup.api.repository.people.PersonRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.notifications.NotificationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrewSwapsService {

    private final CrewSwapRepository crewSwapRepository;
    private final ProjectRepository projectRepository;
    private final PersonRepository personRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final NotificationsService notificationsService;

    @Transactional
    public CrewSwap create(CrewSwap swap, String orgId, Long projectId, Long personId) {
        Organization org = authHelper.validateOrg(orgId);
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        swap.setOrganization(org);
        swap.setProject(project);
        swap.setPerson(person);
        swap.setStatus(CrewSwapStatus.REQUESTED);
        if (swap.getRequestedAt() == null) {
            swap.setRequestedAt(LocalDateTime.now());
        }

        CrewSwap saved = crewSwapRepository.save(swap);
        auditLogService.logMutation("requested", "crew_swap", saved.getId().toString(), orgId, swap.getRequestedBy(),
            Map.of("fromCrewId", swap.getFromCrewId(), "toCrewId", swap.getToCrewId()), Map.of());
        notifyIfPossible(orgId, swap.getRequestedBy(), "crew_swap.requested", Map.of("swapId", saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<CrewSwap> list(String orgId, Long projectId, Long personId, String fromCrewId, String toCrewId,
                               String status, Boolean includeArchived, int page, int limit) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);

        Long orgIdLong = Long.parseLong(orgId);
        Specification<CrewSwap> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("organization").get("id"), orgIdLong)
        );

        if (projectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("project").get("id"), projectId));
        }
        if (personId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("person").get("id"), personId));
        }
        if (fromCrewId != null && !fromCrewId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("fromCrewId"), fromCrewId.trim()));
        }
        if (toCrewId != null && !toCrewId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("toCrewId"), toCrewId.trim()));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), CrewSwapStatus.fromValue(status.trim().toLowerCase())));
        }
        if (includeArchived == null || !includeArchived) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("archivedAt")));
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit);
        return crewSwapRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public CrewSwap getById(Long id, String orgId, boolean includeArchived) {
        CrewSwap swap = crewSwapRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Crew swap not found"));

        if (orgId != null && (swap.getOrganization() == null ||
            !swap.getOrganization().getId().toString().equals(orgId))) {
            throw new ForbiddenException("Cannot access crew swaps outside your organization");
        }

        if (!includeArchived && swap.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Crew swap not found");
        }

        return swap;
    }

    @Transactional
    public CrewSwap approve(Long id, String orgId, String approverId, String comments) {
        CrewSwap swap = getById(id, orgId, false);
        authHelper.ensureNotOnLegalHold(swap, "approve crew swap");

        if (swap.getStatus() != CrewSwapStatus.REQUESTED) {
            throw new BadRequestException("Only requested swaps can be approved");
        }

        swap.setStatus(CrewSwapStatus.APPROVED);
        swap.setApprovedBy(approverId);
        swap.setApprovedAt(LocalDateTime.now());

        CrewSwap saved = crewSwapRepository.save(swap);
        Map<String, Object> approveMetadata = new java.util.HashMap<>();
        if (comments != null && !comments.isBlank()) {
            approveMetadata.put("comments", comments);
        }
        auditLogService.logMutation("approved", "crew_swap", saved.getId().toString(), orgId, approverId,
            approveMetadata, Map.of());
        notifyIfPossible(orgId, approverId, "crew_swap.approved", Map.of("swapId", saved.getId()));
        return saved;
    }

    @Transactional
    public CrewSwap reject(Long id, String orgId, String approverId, String reason) {
        CrewSwap swap = getById(id, orgId, false);
        authHelper.ensureNotOnLegalHold(swap, "reject crew swap");

        if (swap.getStatus() != CrewSwapStatus.REQUESTED) {
            throw new BadRequestException("Only requested swaps can be rejected");
        }

        swap.setStatus(CrewSwapStatus.REJECTED);
        swap.setRejectedBy(approverId);
        swap.setRejectedAt(LocalDateTime.now());
        swap.setRejectionReason(reason);

        CrewSwap saved = crewSwapRepository.save(swap);
        Map<String, Object> rejectMetadata = new java.util.HashMap<>();
        if (reason != null && !reason.isBlank()) {
            rejectMetadata.put("reason", reason);
        }
        auditLogService.logMutation("rejected", "crew_swap", saved.getId().toString(), orgId, approverId,
            rejectMetadata, Map.of());
        notifyIfPossible(orgId, approverId, "crew_swap.rejected", Map.of("swapId", saved.getId()));
        return saved;
    }

    @Transactional
    public CrewSwap complete(Long id, String orgId, String completedBy, String comments) {
        CrewSwap swap = getById(id, orgId, false);
        authHelper.ensureNotOnLegalHold(swap, "complete crew swap");

        if (swap.getStatus() != CrewSwapStatus.APPROVED) {
            throw new BadRequestException("Only approved swaps can be completed");
        }

        swap.setStatus(CrewSwapStatus.COMPLETED);
        swap.setCompletedBy(completedBy);
        swap.setCompletedAt(LocalDateTime.now());

        CrewSwap saved = crewSwapRepository.save(swap);
        Map<String, Object> completeMetadata = new java.util.HashMap<>();
        if (comments != null && !comments.isBlank()) {
            completeMetadata.put("comments", comments);
        }
        auditLogService.logMutation("completed", "crew_swap", saved.getId().toString(), orgId, completedBy,
            completeMetadata, Map.of());
        notifyIfPossible(orgId, completedBy, "crew_swap.completed", Map.of("swapId", saved.getId()));
        return saved;
    }

    @Transactional
    public CrewSwap cancel(Long id, String orgId, String canceledBy, String reason) {
        CrewSwap swap = getById(id, orgId, false);
        authHelper.ensureNotOnLegalHold(swap, "cancel crew swap");

        if (swap.getStatus() == CrewSwapStatus.COMPLETED) {
            throw new BadRequestException("Completed swaps cannot be canceled");
        }

        swap.setStatus(CrewSwapStatus.CANCELED);
        swap.setRejectionReason(reason);

        CrewSwap saved = crewSwapRepository.save(swap);
        auditLogService.logMutation("canceled", "crew_swap", saved.getId().toString(), orgId, canceledBy,
            Map.of("reason", reason), Map.of());
        notifyIfPossible(orgId, canceledBy, "crew_swap.canceled", Map.of("swapId", saved.getId()));
        return saved;
    }

    @Transactional
    public CrewSwap archive(Long id, String orgId) {
        CrewSwap swap = getById(id, orgId, true);
        authHelper.ensureNotOnLegalHold(swap, "archive crew swap");

        swap.setStatus(CrewSwapStatus.ARCHIVED);
        swap.setArchivedAt(LocalDateTime.now());

        CrewSwap saved = crewSwapRepository.save(swap);
        auditLogService.logMutation("archived", "crew_swap", saved.getId().toString(), orgId, null, Map.of(), Map.of());
        notifyIfPossible(orgId, null, "crew_swap.archived", Map.of("swapId", saved.getId()));
        return saved;
    }

    @Transactional
    public CrewSwap unarchive(Long id, String orgId) {
        CrewSwap swap = getById(id, orgId, true);
        authHelper.ensureNotOnLegalHold(swap, "unarchive crew swap");

        swap.setArchivedAt(null);
        if (swap.getStatus() == CrewSwapStatus.ARCHIVED) {
            swap.setStatus(CrewSwapStatus.REQUESTED);
        }

        CrewSwap saved = crewSwapRepository.save(swap);
        auditLogService.logMutation("unarchived", "crew_swap", saved.getId().toString(), orgId, null, Map.of(), Map.of());
        notifyIfPossible(orgId, null, "crew_swap.unarchived", Map.of("swapId", saved.getId()));
        return saved;
    }

    private void notifyIfPossible(String orgId, String userId, String type, Map<String, Object> payload) {
        Long parsedUserId = parseUserId(userId);
        if (parsedUserId == null) {
            return;
        }
        try {
            notificationsService.create(orgId, parsedUserId, type, payload);
        } catch (Exception ex) {
            log.debug("Notification skipped for type {}: {}", type, ex.getMessage());
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
