package com.mytegroup.api.service.crew;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.crew.CrewAssignment;
import com.mytegroup.api.entity.enums.crew.CrewAssignmentStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.crew.CrewAssignmentRepository;
import com.mytegroup.api.repository.people.PersonRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
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
class CrewAssignmentsServiceTest {

    @Mock
    private CrewAssignmentRepository crewAssignmentRepository;

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
    private CrewAssignmentsService crewAssignmentsService;

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
    void testCreate_WithMissingStartDate_ThrowsBadRequestException() {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setCreatedBy("1");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));

        assertThrows(BadRequestException.class, () -> crewAssignmentsService.create(assignment, "1", 2L, 3L));
    }

    @Test
    void testCreate_WithEndDateBeforeStartDate_ThrowsBadRequestException() {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setStartDate(LocalDate.now());
        assignment.setEndDate(LocalDate.now().minusDays(1));
        assignment.setCreatedBy("1");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));

        assertThrows(BadRequestException.class, () -> crewAssignmentsService.create(assignment, "1", 2L, 3L));
    }

    @Test
    void testCreate_WithOverlap_ThrowsConflictException() {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setStartDate(LocalDate.now());
        assignment.setEndDate(LocalDate.now());
        assignment.setCreatedBy("1");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));
        when(crewAssignmentRepository.findOverlappingAssignments(1L, 3L, assignment.getStartDate(), assignment.getEndDate()))
            .thenReturn(List.of(new CrewAssignment()));

        assertThrows(ConflictException.class, () -> crewAssignmentsService.create(assignment, "1", 2L, 3L));
    }

    @Test
    void testCreate_WithInvalidCreatedBy_SkipsNotification() {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setStartDate(LocalDate.now());
        assignment.setEndDate(LocalDate.now());
        assignment.setCrewId("crew-1");
        assignment.setCreatedBy("not-a-number");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));
        when(crewAssignmentRepository.findOverlappingAssignments(anyLong(), anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of());
        when(crewAssignmentRepository.save(any(CrewAssignment.class))).thenAnswer(invocation -> {
            CrewAssignment saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        crewAssignmentsService.create(assignment, "1", 2L, 3L);

        verify(notificationsService, never()).create(anyString(), anyLong(), anyString(), anyMap());
    }

    @Test
    void testList_WithMissingOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> crewAssignmentsService.list(
            null, null, null, null, null, null, null, null, 0, 25));
    }

    @Test
    void testList_WithFilters_ReturnsPage() {
        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(crewAssignmentRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        assertNotNull(crewAssignmentsService.list(
            "1", 2L, 3L, " crew-1 ", LocalDate.now().minusDays(1), LocalDate.now(), "active", false, 0, 25));
    }

    @Test
    void testUpdate_WithEndDateBeforeStartDate_ThrowsBadRequestException() {
        CrewAssignment existing = new CrewAssignment();
        existing.setId(1L);
        existing.setOrganization(testOrg);
        existing.setStartDate(LocalDate.now());

        CrewAssignment updates = new CrewAssignment();
        updates.setEndDate(LocalDate.now().minusDays(1));

        when(crewAssignmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> crewAssignmentsService.update(1L, updates, "1"));
    }

    @Test
    void testGetById_WithArchivedNotIncluded_ThrowsResourceNotFoundException() {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setId(1L);
        assignment.setOrganization(testOrg);
        assignment.setArchivedAt(LocalDateTime.now());

        when(crewAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));

        assertThrows(ResourceNotFoundException.class, () -> crewAssignmentsService.getById(1L, "1", false));
    }
}
