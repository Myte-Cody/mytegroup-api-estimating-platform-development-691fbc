package com.mytegroup.api.service.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.OrganizationRepository;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.seats.SeatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private SeatsService seatsService;

    @InjectMocks
    private UsersService usersService;

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usersService, "defaultSeatsPerOrg", 5);

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setOrganization(testOrganization);
        testUser.setRole(Role.USER);
        testUser.setRoles(new ArrayList<>(List.of(Role.USER)));
    }

    @Test
    void testCreate_WithValidUser_CreatesUser() {
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setUsername("newuser");
        newUser.setOrganization(testOrganization);
        newUser.setPasswordHash("ValidPassword123!@");
        // Don't set roles - let the service create them

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            // Ensure roles is a mutable list (service creates it, but ensure it's mutable)
            if (user.getRoles() != null) {
                user.setRoles(new ArrayList<>(user.getRoles()));
            } else {
                user.setRoles(new ArrayList<>());
            }
            return user;
        });
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), anyString(), any());

        User result = usersService.create(newUser, false);

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditLogService, times(1)).log(anyString(), anyString(), isNull(), anyString(), anyString(), isNull());
    }

    @Test
    void testCreate_WithExistingEmail_ThrowsConflictException() {
        User newUser = new User();
        newUser.setEmail("existing@example.com");
        newUser.setOrganization(testOrganization);

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> {
            usersService.create(newUser, false);
        });
    }

    @Test
    void testCreate_WithNoEmail_ThrowsBadRequestException() {
        User newUser = new User();
        newUser.setOrganization(testOrganization);

        assertThrows(BadRequestException.class, () -> {
            usersService.create(newUser, false);
        });
    }

    @Test
    void testCreate_WithNoOrganization_ThrowsBadRequestException() {
        User newUser = new User();
        newUser.setEmail("test@example.com");

        assertThrows(BadRequestException.class, () -> {
            usersService.create(newUser, false);
        });
    }

    @Test
    void testCreate_WithWeakPassword_ThrowsBadRequestException() {
        User newUser = new User();
        newUser.setEmail("test@example.com");
        newUser.setOrganization(testOrganization);
        newUser.setPasswordHash("weak");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            usersService.create(newUser, false);
        });
    }

    @Test
    void testCreate_WithEnforceSeat_AllocatesSeat() {
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setUsername("newuser");
        newUser.setOrganization(testOrganization);
        newUser.setPasswordHash("ValidPassword123!@");
        newUser.setRole(Role.USER);

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            // Ensure roles is a mutable list
            if (user.getRoles() != null) {
                user.setRoles(new ArrayList<>(user.getRoles()));
            }
            return user;
        });
        doNothing().when(seatsService).ensureOrgSeats(anyString(), anyInt());
        when(seatsService.allocateSeat(anyString(), anyLong(), anyString(), any())).thenReturn(null);
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), anyString(), any());

        User result = usersService.create(newUser, true);

        assertNotNull(result);
        verify(seatsService, times(1)).ensureOrgSeats(eq("1"), eq(5));
        verify(seatsService, times(1)).allocateSeat(eq("1"), eq(1L), anyString(), isNull());
        verify(auditLogService, times(1)).log(anyString(), anyString(), any(), anyString(), anyString(), any());
    }

    @Test
    void testFindByEmail_WithValidEmail_ReturnsUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = usersService.findByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testFindByEmail_WithArchivedUser_ReturnsNull() {
        testUser.setArchivedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = usersService.findByEmail("test@example.com");

        assertNull(result);
    }

    @Test
    void testFindByEmail_WithNonExistentEmail_ReturnsNull() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        User result = usersService.findByEmail("nonexistent@example.com");

        assertNull(result);
    }

    @Test
    void testFindAnyByEmail_WithArchivedUser_ReturnsUser() {
        testUser.setArchivedAt(LocalDateTime.now());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = usersService.findAnyByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testList_WithValidOrgId_ReturnsUsers() {
        when(userRepository.findByOrganization_IdAndArchivedAtIsNull(eq(1L), any(Pageable.class)))
            .thenReturn(Page.empty());

        List<User> result = usersService.list("1", false);

        assertNotNull(result);
        verify(userRepository, times(1)).findByOrganization_IdAndArchivedAtIsNull(eq(1L), any(Pageable.class));
    }

    @Test
    void testList_WithIncludeArchived_ReturnsAllUsers() {
        when(userRepository.findByOrganization_Id(1L)).thenReturn(List.of(testUser));

        List<User> result = usersService.list("1", true);

        assertNotNull(result);
        verify(userRepository, times(1)).findByOrganization_Id(1L);
    }

    @Test
    void testList_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            usersService.list(null, false);
        });
    }

    @Test
    void testGetById_WithValidId_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = usersService.getById(1L, false);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetById_WithArchivedUserAndIncludeArchivedFalse_ThrowsResourceNotFoundException() {
        testUser.setArchivedAt(LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(ResourceNotFoundException.class, () -> {
            usersService.getById(1L, false);
        });
    }

    @Test
    void testGetById_WithArchivedUserAndIncludeArchivedTrue_ReturnsUser() {
        testUser.setArchivedAt(LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = usersService.getById(1L, true);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            usersService.getById(999L, false);
        });
    }

    @Test
    void testGetByIdForSession_WithValidId_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = usersService.getByIdForSession(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testFindByVerificationToken_WithValidToken_ReturnsUser() {
        when(userRepository.findByVerificationTokenHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.of(testUser));

        User result = usersService.findByVerificationToken("valid-token-hash");

        assertNotNull(result);
        assertEquals(testUser, result);
    }

    @Test
    void testFindByVerificationToken_WithInvalidToken_ThrowsBadRequestException() {
        when(userRepository.findByVerificationTokenHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> {
            usersService.findByVerificationToken("invalid-token-hash");
        });
    }

    @Test
    void testSetVerificationToken_SetsToken() {
        when(userRepository.setVerificationToken(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(1);

        usersService.setVerificationToken(1L, "token-hash", LocalDateTime.now().plusHours(24));

        verify(userRepository, times(1)).setVerificationToken(eq(1L), eq("token-hash"), any(LocalDateTime.class));
    }

    @Test
    void testClearVerificationToken_ClearsTokenAndReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.clearVerificationToken(1L)).thenReturn(1);

        User result = usersService.clearVerificationToken(1L);

        assertNotNull(result);
        verify(userRepository, times(1)).clearVerificationToken(1L);
    }

    @Test
    void testFindByResetToken_WithValidToken_ReturnsUser() {
        when(userRepository.findByResetTokenHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.of(testUser));

        User result = usersService.findByResetToken("valid-reset-token-hash");

        assertNotNull(result);
        assertEquals(testUser, result);
    }

    @Test
    void testFindByResetToken_WithInvalidToken_ThrowsBadRequestException() {
        when(userRepository.findByResetTokenHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> {
            usersService.findByResetToken("invalid-reset-token-hash");
        });
    }

    @Test
    void testSetResetToken_SetsToken() {
        when(userRepository.setResetToken(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(1);

        usersService.setResetToken(1L, "reset-token-hash", LocalDateTime.now().plusHours(1));

        verify(userRepository, times(1)).setResetToken(eq(1L), eq("reset-token-hash"), any(LocalDateTime.class));
    }

    @Test
    void testClearResetTokenAndSetPassword_WithValidPassword_UpdatesPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.clearResetTokenAndSetPassword(anyLong(), anyString())).thenReturn(1);

        User result = usersService.clearResetTokenAndSetPassword(1L, "NewValidPassword123!@");

        assertNotNull(result);
        verify(userRepository, times(1)).clearResetTokenAndSetPassword(eq(1L), eq("encoded-password"));
    }

    @Test
    void testClearResetTokenAndSetPassword_WithWeakPassword_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            usersService.clearResetTokenAndSetPassword(1L, "weak");
        });
    }

    @Test
    void testMarkLastLogin_UpdatesLastLogin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.updateLastLogin(anyLong(), any(LocalDateTime.class))).thenReturn(1);

        User result = usersService.markLastLogin(1L);

        assertNotNull(result);
        verify(userRepository, times(1)).updateLastLogin(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void testUpdate_WithValidUpdates_UpdatesUser() {
        User updates = new User();
        updates.setEmail("updated@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = usersService.update(1L, updates);

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdate_WithArchivedUser_ThrowsForbiddenException() {
        testUser.setArchivedAt(LocalDateTime.now());
        User updates = new User();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(ForbiddenException.class, () -> {
            usersService.update(1L, updates);
        });
    }

    @Test
    void testUpdate_WithNonExistentUser_ThrowsResourceNotFoundException() {
        User updates = new User();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            usersService.update(999L, updates);
        });
    }
}


