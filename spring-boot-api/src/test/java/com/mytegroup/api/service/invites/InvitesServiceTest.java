package com.mytegroup.api.service.invites;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.core.InviteStatus;
import com.mytegroup.api.entity.enums.people.PersonType;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.InviteRepository;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.email.EmailService;
import com.mytegroup.api.service.notifications.NotificationsService;
import com.mytegroup.api.service.persons.PersonsService;
import com.mytegroup.api.service.seats.SeatsService;
import com.mytegroup.api.service.users.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitesServiceTest {

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsersService usersService;

    @Mock
    private PersonsService personsService;

    @Mock
    private SeatsService seatsService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationsService notificationsService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private InvitesService invitesService;

    private Organization testOrganization;
    private Person testPerson;
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitesService, "defaultExpiryHours", 72);
        ReflectionTestUtils.setField(invitesService, "throttleWindowMinutes", 10);
        ReflectionTestUtils.setField(invitesService, "defaultSeatsPerOrg", 5);

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testPerson = new Person();
        testPerson.setId(1L);
        testPerson.setPrimaryEmail("person@example.com");
        testPerson.setPersonType(PersonType.INTERNAL_STAFF);
        testPerson.setOrganization(testOrganization);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");
    }

    @Test
    void testCreate_WithValidData_CreatesInvite() {
        Long personId = 1L;
        Role role = Role.USER;
        int expiresInHours = 72;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(inviteRepository.findExpiredPendingInvites(eq(1L), any(java.time.LocalDateTime.class)))
            .thenReturn(List.of());
        when(personsService.getById(personId, orgId, false)).thenReturn(testPerson);
        when(inviteRepository.findPendingActiveInvite(eq(1L), eq("person@example.com"), any(java.time.LocalDateTime.class)))
            .thenReturn(Optional.empty());
        when(inviteRepository.countRecentInvites(eq(1L), eq("person@example.com"), any(java.time.LocalDateTime.class)))
            .thenReturn(0L);
        when(usersService.findAnyByEmail("person@example.com")).thenReturn(null);
        // Mock fallback user lookup for createdByUser
        when(userRepository.findByOrganization_Id(1L)).thenReturn(List.of(testUser));
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> {
            Invite invite = invocation.getArgument(0);
            invite.setId(1L);
            return invite;
        });
        doNothing().when(emailService).sendInviteEmail(anyString(), anyString(), anyString(), anyString(), anyString());
        doNothing().when(seatsService).ensureOrgSeats(anyString(), anyInt());
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), anyString(), any());

        Invite result = invitesService.create(personId, role, expiresInHours, orgId);

        assertNotNull(result);
        verify(inviteRepository, times(1)).save(any(Invite.class));
        verify(emailService, atLeastOnce()).sendInviteEmail(eq("person@example.com"), anyString(), eq(orgId), eq(testOrganization.getName()), isNull());
    }

    @Test
    void testCreate_WithSuperAdminRole_ThrowsForbiddenException() {
        Long personId = 1L;
        Role role = Role.SUPER_ADMIN;
        String orgId = "1";

        assertThrows(ForbiddenException.class, () -> {
            invitesService.create(personId, role, 72, orgId);
        });
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        Long personId = 1L;
        Role role = Role.USER;

        assertThrows(BadRequestException.class, () -> {
            invitesService.create(personId, role, 72, null);
        });
    }

    @Test
    void testCreate_WithPersonAlreadyLinkedToUser_ThrowsConflictException() {
        Long personId = 1L;
        Role role = Role.USER;
        String orgId = "1";
        testPerson.setUser(testUser);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(personsService.getById(personId, orgId, false)).thenReturn(testPerson);

        assertThrows(ConflictException.class, () -> {
            invitesService.create(personId, role, 72, orgId);
        });
    }

    @Test
    void testCreate_WithPersonWithoutEmail_ThrowsBadRequestException() {
        Long personId = 1L;
        Role role = Role.USER;
        String orgId = "1";
        testPerson.setPrimaryEmail(null);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(personsService.getById(personId, orgId, false)).thenReturn(testPerson);

        assertThrows(BadRequestException.class, () -> {
            invitesService.create(personId, role, 72, orgId);
        });
    }

    @Test
    void testCreate_WithPendingInvite_ThrowsConflictException() {
        Long personId = 1L;
        Role role = Role.USER;
        String orgId = "1";

        Invite pendingInvite = new Invite();
        pendingInvite.setStatus(InviteStatus.PENDING);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(personsService.getById(personId, orgId, false)).thenReturn(testPerson);
        when(usersService.findAnyByEmail("person@example.com")).thenReturn(null);
        when(inviteRepository.findPendingActiveInvite(eq(1L), eq("person@example.com"), any(java.time.LocalDateTime.class)))
            .thenReturn(Optional.of(pendingInvite));

        assertThrows(ConflictException.class, () -> {
            invitesService.create(personId, role, 72, orgId);
        });
    }

    @Test
    void testCreate_WithExistingUser_ThrowsConflictException() {
        Long personId = 1L;
        Role role = Role.USER;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(personsService.getById(personId, orgId, false)).thenReturn(testPerson);
        when(usersService.findAnyByEmail("person@example.com")).thenReturn(testUser);

        assertThrows(ConflictException.class, () -> {
            invitesService.create(personId, role, 72, orgId);
        });
    }

    @Test
    void testResend_WithValidInvite_ResendsInvite() {
        Long inviteId = 1L;
        String orgId = "1";

        Invite invite = new Invite();
        invite.setId(inviteId);
        invite.setOrganization(testOrganization);
        invite.setEmail("person@example.com");
        invite.setStatus(InviteStatus.PENDING);
        invite.setTokenExpires(LocalDateTime.now().plusHours(24));

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(inviteRepository.findExpiredPendingInvites(eq(1L), any(java.time.LocalDateTime.class)))
            .thenReturn(List.of());
        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendInviteEmail(anyString(), anyString(), anyString(), anyString(), anyString());
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), anyString(), any());

        Invite result = invitesService.resend(inviteId, orgId);

        assertNotNull(result);
        verify(inviteRepository, times(1)).save(any(Invite.class));
        verify(emailService, atLeastOnce()).sendInviteEmail(eq("person@example.com"), anyString(), eq(orgId), eq(testOrganization.getName()), isNull());
    }

    @Test
    void testResend_WithNonExistentInvite_ThrowsResourceNotFoundException() {
        Long inviteId = 999L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(inviteRepository.findById(inviteId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            invitesService.resend(inviteId, orgId);
        });
    }

    @Test
    void testCancel_WithValidInvite_CancelsInvite() {
        Long inviteId = 1L;
        String orgId = "1";

        Invite invite = new Invite();
        invite.setId(inviteId);
        invite.setOrganization(testOrganization);
        invite.setStatus(InviteStatus.PENDING);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invite result = invitesService.cancel(inviteId, orgId);

        assertNotNull(result);
        // Cancel sets archivedAt, not status
        assertNotNull(result.getArchivedAt());
        verify(inviteRepository, times(1)).save(any(Invite.class));
    }

    @Test
    void testCancel_WithNonExistentInvite_ThrowsResourceNotFoundException() {
        Long inviteId = 999L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(inviteRepository.findById(inviteId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            invitesService.cancel(inviteId, orgId);
        });
    }
}

