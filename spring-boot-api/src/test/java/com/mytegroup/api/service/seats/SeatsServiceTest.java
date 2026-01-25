package com.mytegroup.api.service.seats;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.projects.SeatStatus;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.projects.Seat;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.repository.projects.SeatRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatsServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private SeatsService seatsService;

    private Organization testOrganization;
    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setOrganization(testOrganization);

        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setOrganization(testOrganization);
    }

    @Test
    void testEnsureOrgSeats_WithValidOrg_CreatesSeats() {
        String orgId = "1";
        int totalSeats = 5;

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_Id(1L)).thenReturn(new ArrayList<>());
        when(seatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        seatsService.ensureOrgSeats(orgId, totalSeats);

        verify(seatRepository, times(1)).saveAll(anyList());
        verify(auditLogService, times(1)).logMutation(anyString(), anyString(), isNull(), eq(orgId), isNull(), any(), isNull());
    }

    @Test
    void testEnsureOrgSeats_WithExistingSeats_CreatesOnlyMissingSeats() {
        String orgId = "1";
        int totalSeats = 5;

        Seat existingSeat1 = new Seat();
        existingSeat1.setSeatNumber(1);
        Seat existingSeat2 = new Seat();
        existingSeat2.setSeatNumber(2);
        List<Seat> existingSeats = List.of(existingSeat1, existingSeat2);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_Id(1L)).thenReturn(existingSeats);
        when(seatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        seatsService.ensureOrgSeats(orgId, totalSeats);

        verify(seatRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testEnsureOrgSeats_WithZeroSeats_DoesNothing() {
        String orgId = "1";
        int totalSeats = 0;

        seatsService.ensureOrgSeats(orgId, totalSeats);

        verify(seatRepository, never()).saveAll(anyList());
    }

    @Test
    void testEnsureOrgSeats_WithNullOrgId_DoesNothing() {
        seatsService.ensureOrgSeats(null, 5);

        verify(seatRepository, never()).saveAll(anyList());
    }

    @Test
    void testAllocateSeat_WithValidInput_AllocatesSeat() {
        String orgId = "1";
        Long userId = 1L;
        String role = "user";
        Long projectId = 1L;

        Seat vacantSeat = new Seat();
        vacantSeat.setId(1L);
        vacantSeat.setOrganization(testOrganization);
        vacantSeat.setSeatNumber(1);
        vacantSeat.setStatus(SeatStatus.VACANT);
        vacantSeat.setHistory(new ArrayList<>());

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.empty());
        when(seatRepository.findByOrganization_IdAndStatusOrderBySeatNumber(1L, SeatStatus.VACANT))
            .thenReturn(List.of(vacantSeat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Seat result = seatsService.allocateSeat(orgId, userId, role, projectId);

        assertNotNull(result);
        assertEquals(SeatStatus.ACTIVE, result.getStatus());
        assertEquals(testUser, result.getUser());
        assertEquals(role, result.getRole());
        assertEquals(testProject, result.getProject());
        assertNotNull(result.getActivatedAt());
        verify(seatRepository, times(1)).save(any(Seat.class));
    }

    @Test
    void testAllocateSeat_WithUserAlreadyHavingSeat_ThrowsConflictException() {
        String orgId = "1";
        Long userId = 1L;

        Seat existingSeat = new Seat();
        existingSeat.setUser(testUser);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.of(existingSeat));

        assertThrows(ConflictException.class, () -> {
            seatsService.allocateSeat(orgId, userId, "user", null);
        });
    }

    @Test
    void testAllocateSeat_WithNoVacantSeats_ThrowsForbiddenException() {
        String orgId = "1";
        Long userId = 1L;

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.empty());
        when(seatRepository.findByOrganization_IdAndStatusOrderBySeatNumber(1L, SeatStatus.VACANT))
            .thenReturn(new ArrayList<>());

        assertThrows(ForbiddenException.class, () -> {
            seatsService.allocateSeat(orgId, userId, "user", null);
        });
    }

    @Test
    void testAllocateSeat_WithNonExistentUser_ThrowsResourceNotFoundException() {
        String orgId = "1";
        Long userId = 1L;

        Seat vacantSeat = new Seat();
        vacantSeat.setStatus(SeatStatus.VACANT);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.empty());
        when(seatRepository.findByOrganization_IdAndStatusOrderBySeatNumber(1L, SeatStatus.VACANT))
            .thenReturn(List.of(vacantSeat));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            seatsService.allocateSeat(orgId, userId, "user", null);
        });
    }

    @Test
    void testAllocateSeat_WithNonExistentProject_ThrowsResourceNotFoundException() {
        String orgId = "1";
        Long userId = 1L;
        Long projectId = 999L;

        Seat vacantSeat = new Seat();
        vacantSeat.setStatus(SeatStatus.VACANT);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.empty());
        when(seatRepository.findByOrganization_IdAndStatusOrderBySeatNumber(1L, SeatStatus.VACANT))
            .thenReturn(List.of(vacantSeat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            seatsService.allocateSeat(orgId, userId, "user", projectId);
        });
    }

    @Test
    void testReleaseSeatForUser_WithValidSeat_ReleasesSeat() {
        String orgId = "1";
        Long userId = 1L;

        Seat activeSeat = new Seat();
        activeSeat.setId(1L);
        activeSeat.setOrganization(testOrganization);
        activeSeat.setStatus(SeatStatus.ACTIVE);
        activeSeat.setUser(testUser);
        activeSeat.setHistory(new ArrayList<>());

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.of(activeSeat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Seat result = seatsService.releaseSeatForUser(orgId, userId);

        assertNotNull(result);
        assertEquals(SeatStatus.VACANT, result.getStatus());
        assertNull(result.getUser());
        assertNull(result.getProject());
        verify(seatRepository, times(1)).save(any(Seat.class));
    }

    @Test
    void testReleaseSeatForUser_WithNoSeatForUser_ThrowsResourceNotFoundException() {
        String orgId = "1";
        Long userId = 1L;

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            seatsService.releaseSeatForUser(orgId, userId);
        });
    }

    @Test
    void testReleaseSeatForUser_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            seatsService.releaseSeatForUser(null, 1L);
        });
    }

    @Test
    void testReleaseSeatForUser_WithNullUserId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            seatsService.releaseSeatForUser("1", null);
        });
    }

    @Test
    void testFindActiveSeatForUser_WithActiveSeat_ReturnsSeat() {
        String orgId = "1";
        Long userId = 1L;

        Seat activeSeat = new Seat();
        activeSeat.setUser(testUser);
        activeSeat.setStatus(SeatStatus.ACTIVE);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.of(activeSeat));

        Optional<Seat> result = seatsService.findActiveSeatForUser(orgId, userId);

        assertTrue(result.isPresent());
        assertEquals(activeSeat, result.get());
    }

    @Test
    void testFindActiveSeatForUser_WithNoSeat_ReturnsEmpty() {
        String orgId = "1";
        Long userId = 1L;

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(seatRepository.findByOrganization_IdAndUserId(1L, userId)).thenReturn(Optional.empty());

        Optional<Seat> result = seatsService.findActiveSeatForUser(orgId, userId);

        assertFalse(result.isPresent());
    }
}

