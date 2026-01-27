package com.mytegroup.api.service.timesheets;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.timesheets.TimesheetStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.timesheets.Timesheet;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.people.PersonRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.repository.timesheets.TimesheetRepository;
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
public class TimesheetsService {

    private final TimesheetRepository timesheetRepository;
    private final ProjectRepository projectRepository;
    private final PersonRepository personRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final NotificationsService notificationsService;

    @Transactional
    public Timesheet create(Timesheet timesheet, String orgId, Long projectId, Long personId) {
        Organization org = authHelper.validateOrg(orgId);

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        if (timesheet.getWorkDate() == null) {
            throw new BadRequestException("Work date is required");
        }

        timesheetRepository.findByOrganization_IdAndProject_IdAndPerson_IdAndWorkDateAndArchivedAtIsNull(
            org.getId(), project.getId(), person.getId(), timesheet.getWorkDate()
        ).ifPresent(existing -> {
            throw new ConflictException("Timesheet already exists for this person/date/project");
        });

        timesheet.setOrganization(org);
        timesheet.setProject(project);
        timesheet.setPerson(person);
        timesheet.setStatus(TimesheetStatus.DRAFT);

        Timesheet saved = timesheetRepository.save(timesheet);

        auditLogService.logMutation(
            "created",
            "timesheet",
            saved.getId().toString(),
            orgId,
            timesheet.getCreatedBy(),
            Map.of("projectId", projectId, "personId", personId, "date", timesheet.getWorkDate()),
            Map.of()
        );
        notifyIfPossible(orgId, timesheet.getCreatedBy(), "timesheet.created", Map.of("timesheetId", saved.getId()));

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Timesheet> list(String orgId, Long projectId, Long personId, String crewId,
                                LocalDate dateFrom, LocalDate dateTo, String status,
                                Boolean includeArchived, int page, int limit) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);

        Long orgIdLong = Long.parseLong(orgId);
        Specification<Timesheet> spec = Specification.where(
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
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), TimesheetStatus.fromValue(status.trim().toLowerCase())));
        }
        if (includeArchived == null || !includeArchived) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("archivedAt")));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("workDate"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("workDate"), dateTo));
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit);

        return timesheetRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Timesheet getById(Long id, String orgId, boolean includeArchived) {
        Timesheet timesheet = timesheetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found"));

        if (orgId != null && (timesheet.getOrganization() == null ||
            !timesheet.getOrganization().getId().toString().equals(orgId))) {
            throw new ForbiddenException("Cannot access timesheets outside your organization");
        }

        if (!includeArchived && timesheet.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Timesheet not found");
        }

        return timesheet;
    }

    @Transactional
    public Timesheet update(Long id, Timesheet updates, String orgId) {
        Timesheet timesheet = getById(id, orgId, true);
        authHelper.ensureNotOnLegalHold(timesheet, "update timesheet");

        if (updates.getEntries() != null) {
            timesheet.getEntries().clear();
            timesheet.getEntries().addAll(updates.getEntries());
        }
        if (updates.getStatus() != null) {
            timesheet.setStatus(updates.getStatus());
        }

        Timesheet saved = timesheetRepository.save(timesheet);
        auditLogService.logMutation(
            "updated",
            "timesheet",
            saved.getId().toString(),
            orgId,
            updates.getCreatedBy(),
            Map.of("status", saved.getStatus().getValue()),
            Map.of()
        );
        return saved;
    }

    @Transactional
    public Timesheet submit(Long id, String orgId, String submittedBy) {
        Timesheet timesheet = getById(id, orgId, false);
        authHelper.ensureNotOnLegalHold(timesheet, "submit timesheet");

        if (timesheet.getStatus() != TimesheetStatus.DRAFT) {
            throw new BadRequestException("Only draft timesheets can be submitted");
        }

        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmittedAt(LocalDateTime.now());

        Timesheet saved = timesheetRepository.save(timesheet);
        auditLogService.logMutation("submitted", "timesheet", saved.getId().toString(), orgId, submittedBy,
            Map.of("status", saved.getStatus().getValue()), Map.of());
        notifyIfPossible(orgId, submittedBy, "timesheet.submitted", Map.of("timesheetId", saved.getId()));
        return saved;
    }

    @Transactional
    public Timesheet approve(Long id, String orgId, String approverId, String comments) {
        Timesheet timesheet = getById(id, orgId, false);
        authHelper.ensureNotOnLegalHold(timesheet, "approve timesheet");

        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BadRequestException("Only submitted timesheets can be approved");
        }

        timesheet.setStatus(TimesheetStatus.APPROVED);
        timesheet.setApprovedBy(approverId);
        timesheet.setApprovedAt(LocalDateTime.now());

        Timesheet saved = timesheetRepository.save(timesheet);
        Map<String, Object> approveMetadata = new java.util.HashMap<>();
        if (comments != null && !comments.isBlank()) {
            approveMetadata.put("comments", comments);
        }
        auditLogService.logMutation("approved", "timesheet", saved.getId().toString(), orgId, approverId,
            approveMetadata, Map.of());
        notifyIfPossible(orgId, approverId, "timesheet.approved", Map.of("timesheetId", saved.getId()));
        return saved;
    }

    @Transactional
    public Timesheet reject(Long id, String orgId, String approverId, String reason) {
        Timesheet timesheet = getById(id, orgId, false);
        authHelper.ensureNotOnLegalHold(timesheet, "reject timesheet");

        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BadRequestException("Only submitted timesheets can be rejected");
        }

        timesheet.setStatus(TimesheetStatus.REJECTED);
        timesheet.setRejectedBy(approverId);
        timesheet.setRejectedAt(LocalDateTime.now());
        timesheet.setRejectionReason(reason);

        Timesheet saved = timesheetRepository.save(timesheet);
        Map<String, Object> rejectMetadata = new java.util.HashMap<>();
        if (reason != null && !reason.isBlank()) {
            rejectMetadata.put("reason", reason);
        }
        auditLogService.logMutation("rejected", "timesheet", saved.getId().toString(), orgId, approverId,
            rejectMetadata, Map.of());
        notifyIfPossible(orgId, approverId, "timesheet.rejected", Map.of("timesheetId", saved.getId()));
        return saved;
    }

    @Transactional
    public Timesheet archive(Long id, String orgId) {
        Timesheet timesheet = getById(id, orgId, true);
        authHelper.ensureNotOnLegalHold(timesheet, "archive timesheet");

        timesheet.setStatus(TimesheetStatus.ARCHIVED);
        timesheet.setArchivedAt(LocalDateTime.now());

        Timesheet saved = timesheetRepository.save(timesheet);
        auditLogService.logMutation("archived", "timesheet", saved.getId().toString(), orgId, null, Map.of(), Map.of());
        notifyIfPossible(orgId, null, "timesheet.archived", Map.of("timesheetId", saved.getId()));
        return saved;
    }

    @Transactional
    public Timesheet unarchive(Long id, String orgId) {
        Timesheet timesheet = getById(id, orgId, true);
        authHelper.ensureNotOnLegalHold(timesheet, "unarchive timesheet");

        timesheet.setArchivedAt(null);
        if (timesheet.getStatus() == TimesheetStatus.ARCHIVED) {
            timesheet.setStatus(TimesheetStatus.DRAFT);
        }

        Timesheet saved = timesheetRepository.save(timesheet);
        auditLogService.logMutation("unarchived", "timesheet", saved.getId().toString(), orgId, null, Map.of(), Map.of());
        notifyIfPossible(orgId, null, "timesheet.unarchived", Map.of("timesheetId", saved.getId()));
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
