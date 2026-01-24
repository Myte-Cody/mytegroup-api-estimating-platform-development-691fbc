package com.mytegroup.api.mapper.auth;

import com.mytegroup.api.dto.auth.RegisterDto;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.common.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class AuthMapperUnitTest {

    private AuthMapper authMapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        authMapper = new AuthMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testRegisterDtoToUserEntity() {
        // Arrange
        RegisterDto dto = new RegisterDto(
            "john@example.com",
            "john_doe",
            "password123",
            "John",
            "Doe",
            Role.USER,
            "1"
        );

        // Act
        User user = authMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(user);
        assertEquals("john@example.com", user.getEmail());
        assertEquals("john_doe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals(organization, user.getOrganization());
        assertFalse(user.getIsEmailVerified());
        assertFalse(user.getPiiStripped());
    }

    @Test
    void testRegisterDtoWithNullValues() {
        // Arrange
        RegisterDto dto = new RegisterDto(
            "test@example.com",
            "testuser",
            "pass123",
            null,
            null,
            Role.USER,
            "1"
        );

        // Act
        User user = authMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
        assertEquals("testuser", user.getUsername());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertEquals(organization, user.getOrganization());
    }

    @Test
    void testRegisterDtoWithAdminRole() {
        // Arrange
        RegisterDto dto = new RegisterDto(
            "admin@example.com",
            "admin",
            "adminpass",
            "Admin",
            "User",
            Role.ADMIN,
            "1"
        );

        // Act
        User user = authMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(user);
        assertEquals(Role.ADMIN, user.getRole());
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().contains(Role.ADMIN));
    }
}

