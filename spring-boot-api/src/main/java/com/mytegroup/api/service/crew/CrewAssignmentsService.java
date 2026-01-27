package com.mytegroup.api.service.crew;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.crew.CrewAssignment;
import com.mytegroup.api.entity.enums.crew.CrewAssignmentStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.crew.CrewAssignmentRepository;
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
public class CrewAssignmentsService {

    private final CrewAssignmentRepository crewAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final PersonRepository personRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final NotificationsService notificationsService;

    @Transactional
    public CrewAssignment create(CrewAssignment assignment, String orgId, Long projectId, Long personId) {
        Organization org = authHelper.validateOrg(orgId);
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        if (assignment.getStartDate() == null) {
            throw new BadRequestException("Start date is required");
        }
        if (assignment.getEndDate() != null && assignment.getEndDate().isBefore(assignment.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        LocalDate endDate = assignment.getEndDate() != null ? assignment.getEndDate() : assignment.getStartDate();
        if (!crewAssignmentRepository.findOverlappingAssignments(org.getId(), person.getId(), assignment.getStartDate(), endDate).isEmpty()) {
            throw new ConflictException("Overlapping crew assignment exists for this person");
        }

        assignment.setOrganization(org);
        assignment.setProject(project);
        assignment.setPerson(person);
        assignment.setStatus(CrewAssignmentStatus.ACTIVE);

        CrewAssignment saved = crewAssignmentRepository.save(assignment);
        auditLogService.logMutation(
            "created",
            "crew_assignment",
            saved.getId().toString(),
            orgId,
            assignment.getCreatedBy(),
            Map.of("projectId", projectId, "personId", personId, "crewId", assignment.getCrewId()),
            Map.of()
        );
        notifyIfPossible(orgId, assignment.getCreatedBy(), "crew_assignment.created", Map.of("assignmentId", saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<CrewAssignment> list(String orgId, Long projectId, Long personId, String crewId,
                                     LocalDate dateFrom, LocalDate dateTo, String status,
                                     Boolean includeArchived, int page, int limit) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);

        Long orgIdLong = Long.parseLong(orgId);
        Specification<CrewAssignment> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("organization").get("id"), orgIdLong)
        );

        if (projectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("project").get("id"), projectId));
        }
        if (personId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("person").get("id"), personId));
        }
        if (crewId != null && !crewId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("crewId"), crewId.trim()));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), CrewAssignmentStatus.fromValue(status.trim().toLowerCase())));
        }
        if (includeArchived == null || !includeArchived) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("archivedAt")));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                cb.isNull(root.get("endDate")),
                cb.lessThanOrEqualTo(root.get("endDate"), dateTo)
            ));
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit);
        return crewAssignmentRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public CrewAssignment getById(Long id, String orgId, boolean includeArchived) {
        CrewAssignment assignment = crewAssignmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Crew assignment not found"));

        if (orgId != null && (assignment.getOrganization() == null ||
            !assignment.getOrganization().getId().toString().equals(orgId))) {
            throw new ForbiddenException("Cannot access crew assignments outside your organization");
        }

        if (!includeArchived && assignment.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Crew assignment not found");
        }

        return assignment;
    }

    @Transactional
    public CrewAssignment update(Long id, CrewAssignment updates, String orgId) {
        CrewAssignment assignment = getById(id, orgId, true);
        authHelper.ensureNotOnLegalHold(assignment, "update crew assignment");

        if (updates.getCrewId() != null) {
            assignment.setCrewId(updates.getCrewId());
        }
        if (updates.getRoleKey() != null) {
            assignment.setRoleKey(updates.getRoleKey());
        }
        if (updates.getStartDate() != null) {
            assignment.setStartDate(updates.getStartDate());
        }
        if (updates.getEndDate() != null) {
            if (updates.getEndDate().isBefore(assignment.getStartDate())) {
                throw new BadRequestException("End date cannot be before start date");
            }
            assignment.setEndDate(updates.getEndDate());
        }
        if (updates.getStatus() != null) {
            assignment.setStatus(updates.getStatus());
        }

        CrewAssignment saved = crewAssignmentRepository.save(assignment);
        auditLogService.logMutation("updated", "crew_assignment", saved.getId().toString(), orgId, updates.getCreatedBy(),
            Map.of("status", saved.getStatus().getValue()), Map.of());
        notifyIfPossible(orgId, updates.getCreatedBy(), "crew_assignment.updated", Map.of("assignmentId", saved.getId()));
        return saved;
    }

    @Transactional
    public CrewAssignment archive(Long id, String orgId) {
        CrewAssignment assignment = getById(id, orgId, true);
        authHelper.ensureNotOnLegalHold(assignment, "archive crew assignment");

        assignment.setStatus(CrewAssignmentStatus.ARCHIVED);
        assignment.setArchivedAt(LocalDateTime.now());

        CrewAssignment saved = crewAssignmentRepository.save(assignment);
        auditLogService.logMutation("archived", "crew_assignment", saved.getId().toString(), orgId, null, Map.of(), Map.of());
        notifyIfPossible(orgId, null, "crew_assignment.archived", Map.of("assignmentId", saved.getId()));
        return saved;
    }

    @Transactional
    public CrewAssignment unarchive(Long id, String orgId) {
        CrewAssignment assignment = getById(id, orgId, true);
        authHelper.ensureNotOnLegalHold(assignment, "unarchive crew assignment");

        assignment.setArchivedAt(null);
        if (assignment.getStatus() == CrewAssignmentStatus.ARCHIVED) {
            assignment.setStatus(CrewAssignmentStatus.ACTIVE);
        }

        CrewAssignment saved = crewAssignmentRepository.save(assignment);
        auditLogService.logMutation("unarchived", "crew_assignment", saved.getId().toString(), orgId, null, Map.of(), Map.of());
        notifyIfPossible(orgId, null, "crew_assignment.unarchived", Map.of("assignmentId", saved.getId()));
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
