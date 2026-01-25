package com.mytegroup.api.service.rbac;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.service.users.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private RbacService rbacService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Organization testOrganization = new Organization();
        testOrganization.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);
        testUser.setRoles(List.of(Role.USER));
        testUser.setOrganization(testOrganization);
    }

    @Test
    void testHierarchy_ReturnsHierarchy() {
        Map<String, Object> result = rbacService.hierarchy();

        assertNotNull(result);
        assertTrue(result.containsKey("roles"));
    }

    @Test
    void testGetUserRoles_WithValidUserId_ReturnsUser() {
        Long userId = 1L;
        when(usersService.getById(userId, false)).thenReturn(testUser);

        User result = rbacService.getUserRoles(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void testListUserRoles_WithValidOrgId_ReturnsUsers() {
        String orgId = "1";
        when(usersService.list(orgId, false)).thenReturn(List.of(testUser));

        List<User> result = rbacService.listUserRoles(orgId);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testListUserRoles_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            rbacService.listUserRoles(null);
        });
    }

    @Test
    void testUpdateUserRoles_WithValidRoles_UpdatesRoles() {
        Long userId = 1L;
        List<Role> newRoles = List.of(Role.USER, Role.ORG_ADMIN);

        when(usersService.updateRoles(userId, newRoles)).thenReturn(testUser);

        User result = rbacService.updateUserRoles(userId, newRoles);

        assertNotNull(result);
        verify(usersService, times(1)).updateRoles(userId, newRoles);
    }

    @Test
    void testUpdateUserRoles_WithNullRoles_ThrowsBadRequestException() {
        Long userId = 1L;

        assertThrows(BadRequestException.class, () -> {
            rbacService.updateUserRoles(userId, null);
        });
    }

    @Test
    void testUpdateUserRoles_WithEmptyRoles_ThrowsBadRequestException() {
        Long userId = 1L;

        assertThrows(BadRequestException.class, () -> {
            rbacService.updateUserRoles(userId, List.of());
        });
    }

    @Test
    void testRevokeRole_WithValidRole_RevokesRole() {
        Long userId = 1L;
        Role roleToRevoke = Role.USER;
        List<Role> initialRoles = List.of(Role.USER, Role.ORG_ADMIN);
        List<Role> remainingRoles = List.of(Role.ORG_ADMIN);
        
        testUser.setRoles(initialRoles);

        when(usersService.getById(userId, false)).thenReturn(testUser);
        when(usersService.updateRoles(userId, remainingRoles)).thenReturn(testUser);

        User result = rbacService.revokeRole(userId, roleToRevoke);

        assertNotNull(result);
        verify(usersService, times(1)).updateRoles(eq(userId), eq(remainingRoles));
    }

    @Test
    void testRevokeRole_WithLastRole_ThrowsBadRequestException() {
        Long userId = 1L;
        Role roleToRevoke = Role.USER;
        testUser.setRoles(List.of(Role.USER));

        when(usersService.getById(userId, false)).thenReturn(testUser);

        assertThrows(BadRequestException.class, () -> {
            rbacService.revokeRole(userId, roleToRevoke);
        });
    }
}


