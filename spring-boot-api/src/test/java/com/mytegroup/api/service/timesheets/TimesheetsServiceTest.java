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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetsServiceTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private NotificationsService notificationsService;

    @InjectMocks
    private TimesheetsService timesheetsService;

    private Organization testOrg;
    private Project testProject;
    private Person testPerson;

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId(1L);

        testProject = new Project();
        testProject.setId(2L);
        testProject.setOrganization(testOrg);

        testPerson = new Person();
        testPerson.setId(3L);
        testPerson.setOrganization(testOrg);
    }

    @Test
    void testCreate_WithMissingWorkDate_ThrowsBadRequestException() {
        Timesheet timesheet = new Timesheet();
        timesheet.setCreatedBy("1");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));

        assertThrows(BadRequestException.class, () -> timesheetsService.create(timesheet, "1", 2L, 3L));
    }

    @Test
    void testCreate_WithDuplicateTimesheet_ThrowsConflictException() {
        Timesheet timesheet = new Timesheet();
        timesheet.setWorkDate(LocalDate.now());
        timesheet.setCreatedBy("1");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));
        when(timesheetRepository.findByOrganization_IdAndProject_IdAndPerson_IdAndWorkDateAndArchivedAtIsNull(
            1L, 2L, 3L, timesheet.getWorkDate()))
            .thenReturn(Optional.of(new Timesheet()));

        assertThrows(ConflictException.class, () -> timesheetsService.create(timesheet, "1", 2L, 3L));
    }

    @Test
    void testCreate_WithInvalidCreatedBy_SkipsNotification() {
        Timesheet timesheet = new Timesheet();
        timesheet.setWorkDate(LocalDate.now());
        timesheet.setCreatedBy("not-a-number");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));
        when(timesheetRepository.findByOrganization_IdAndProject_IdAndPerson_IdAndWorkDateAndArchivedAtIsNull(
            anyLong(), anyLong(), anyLong(), any(LocalDate.class)))
            .thenReturn(Optional.empty());
        when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> {
            Timesheet saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        timesheetsService.create(timesheet, "1", 2L, 3L);

        verify(notificationsService, never()).create(anyString(), anyLong(), anyString(), anyMap());
    }

    @Test
    void testList_WithMissingOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> timesheetsService.list(
            null, null, null, null, null, null, null, null, 0, 25));
    }

    @Test
    void testList_WithFilters_ReturnsPage() {
        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(timesheetRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        assertNotNull(timesheetsService.list(
            "1", 2L, 3L, " crew-1 ", LocalDate.now().minusDays(2), LocalDate.now(), "approved", false, 0, 25));
    }

    @Test
    void testGetById_WithOrgMismatch_ThrowsForbiddenException() {
        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);
        timesheet.setOrganization(testOrg);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));

        assertThrows(ForbiddenException.class, () -> timesheetsService.getById(1L, "2", true));
    }

    @Test
    void testGetById_WithArchivedNotIncluded_ThrowsResourceNotFoundException() {
        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);
        timesheet.setOrganization(testOrg);
        timesheet.setArchivedAt(LocalDateTime.now());

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));

        assertThrows(ResourceNotFoundException.class, () -> timesheetsService.getById(1L, "1", false));
    }

    @Test
    void testSubmit_WithNonDraftStatus_ThrowsBadRequestException() {
        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);
        timesheet.setOrganization(testOrg);
        timesheet.setStatus(TimesheetStatus.APPROVED);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));

        assertThrows(BadRequestException.class, () -> timesheetsService.submit(1L, "1", "1"));
    }

    @Test
    void testApprove_WithNonSubmittedStatus_ThrowsBadRequestException() {
        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);
        timesheet.setOrganization(testOrg);
        timesheet.setStatus(TimesheetStatus.DRAFT);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));

        assertThrows(BadRequestException.class, () -> timesheetsService.approve(1L, "1", "1", null));
    }
}
