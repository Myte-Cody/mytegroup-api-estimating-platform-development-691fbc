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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrewSwapsServiceTest {

    @Mock
    private CrewSwapRepository crewSwapRepository;

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
    private CrewSwapsService crewSwapsService;

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
    void testCreate_WithNullRequestedAt_SetsTimestamp() {
        CrewSwap swap = new CrewSwap();
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedBy("1");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));
        when(crewSwapRepository.save(any(CrewSwap.class))).thenAnswer(invocation -> {
            CrewSwap saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        CrewSwap result = crewSwapsService.create(swap, "1", 2L, 3L);

        assertNotNull(result.getRequestedAt());
        assertEquals(CrewSwapStatus.REQUESTED, result.getStatus());
    }

    @Test
    void testCreate_WithInvalidRequestedBy_SkipsNotification() {
        CrewSwap swap = new CrewSwap();
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedBy("not-a-number");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject));
        when(personRepository.findById(3L)).thenReturn(Optional.of(testPerson));
        when(crewSwapRepository.save(any(CrewSwap.class))).thenAnswer(invocation -> {
            CrewSwap saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        crewSwapsService.create(swap, "1", 2L, 3L);

        verify(notificationsService, never()).create(anyString(), anyLong(), anyString(), anyMap());
    }

    @Test
    void testList_WithMissingOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> crewSwapsService.list(
            null, null, null, null, null, null, null, 0, 25));
    }

    @Test
    void testList_WithFilters_ReturnsPage() {
        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(crewSwapRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        assertNotNull(crewSwapsService.list(
            "1", 2L, 3L, " crew-a ", " crew-b ", "approved", false, 0, 25));
    }

    @Test
    void testGetById_WithOrgMismatch_ThrowsForbiddenException() {
        CrewSwap swap = new CrewSwap();
        swap.setId(1L);
        swap.setOrganization(testOrg);

        when(crewSwapRepository.findById(1L)).thenReturn(Optional.of(swap));

        assertThrows(ForbiddenException.class, () -> crewSwapsService.getById(1L, "2", true));
    }

    @Test
    void testApprove_WithWrongStatus_ThrowsBadRequestException() {
        CrewSwap swap = new CrewSwap();
        swap.setId(1L);
        swap.setOrganization(testOrg);
        swap.setStatus(CrewSwapStatus.APPROVED);

        when(crewSwapRepository.findById(1L)).thenReturn(Optional.of(swap));

        assertThrows(BadRequestException.class, () -> crewSwapsService.approve(1L, "1", "1", null));
    }

    @Test
    void testComplete_WithWrongStatus_ThrowsBadRequestException() {
        CrewSwap swap = new CrewSwap();
        swap.setId(1L);
        swap.setOrganization(testOrg);
        swap.setStatus(CrewSwapStatus.REQUESTED);

        when(crewSwapRepository.findById(1L)).thenReturn(Optional.of(swap));

        assertThrows(BadRequestException.class, () -> crewSwapsService.complete(1L, "1", "1", null));
    }

    @Test
    void testCancel_WithCompletedStatus_ThrowsBadRequestException() {
        CrewSwap swap = new CrewSwap();
        swap.setId(1L);
        swap.setOrganization(testOrg);
        swap.setStatus(CrewSwapStatus.COMPLETED);

        when(crewSwapRepository.findById(1L)).thenReturn(Optional.of(swap));

        assertThrows(BadRequestException.class, () -> crewSwapsService.cancel(1L, "1", "1", "reason"));
    }

    @Test
    void testUnarchive_WithArchivedStatus_ResetsStatus() {
        CrewSwap swap = new CrewSwap();
        swap.setId(1L);
        swap.setOrganization(testOrg);
        swap.setStatus(CrewSwapStatus.ARCHIVED);
        swap.setArchivedAt(LocalDateTime.now());

        when(crewSwapRepository.findById(1L)).thenReturn(Optional.of(swap));
        when(crewSwapRepository.save(any(CrewSwap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CrewSwap result = crewSwapsService.unarchive(1L, "1");

        assertNull(result.getArchivedAt());
        assertEquals(CrewSwapStatus.REQUESTED, result.getStatus());
    }
}
