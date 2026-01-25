package com.mytegroup.api.mapper.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.users.CreateUserDto;
import com.mytegroup.api.dto.users.UpdateUserDto;
import com.mytegroup.api.dto.response.UserResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperUnitTest {

    private UserMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testCreateUserDtoToEntity() {
        // Arrange
        CreateUserDto dto = new CreateUserDto();
        dto.setUsername("johndoe");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john@example.com");
        dto.setRole(Role.ADMIN);
        dto.setIsEmailVerified(false);
        dto.setIsOrgOwner(false);

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(user);
        assertEquals("johndoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals(organization, user.getOrganization());
        assertEquals(Role.ADMIN, user.getRole());
        assertTrue(user.getRoles().contains(Role.ADMIN));
        assertFalse(user.getIsEmailVerified());
        assertFalse(user.getIsOrgOwner());
    }

    @Test
    void testCreateUserDtoToEntityWithDefaults() {
        // Arrange
        CreateUserDto dto = new CreateUserDto();
        dto.setUsername("simpleuser");
        dto.setFirstName("Simple");
        dto.setLastName("User");
        dto.setEmail("simple@example.com");

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(Role.USER, user.getRole());
        assertTrue(user.getRoles().contains(Role.USER));
        assertFalse(user.getIsEmailVerified());
        assertFalse(user.getIsOrgOwner());
    }

    @Test
    void testCreateUserDtoToEntityWithRoles() {
        // Arrange
        CreateUserDto dto = new CreateUserDto();
        dto.setUsername("multiuser");
        dto.setFirstName("Multi");
        dto.setLastName("User");
        dto.setEmail("multi@example.com");
        dto.setRole(Role.USER);
        dto.setRoles(Arrays.asList(Role.USER, Role.VIEWER));

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(Role.USER));
        assertTrue(user.getRoles().contains(Role.VIEWER));
    }

    @Test
    void testCreateUserDtoToEntitySetsPiiAndLegalHold() {
        // Arrange
        CreateUserDto dto = new CreateUserDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertFalse(user.getPiiStripped());
        assertFalse(user.getLegalHold());
    }

    @Test
    void testUpdateEntity() {
        // Arrange
        User user = new User();
        user.setUsername("olduser");
        user.setFirstName("Old");
        user.setLastName("User");
        user.setEmail("old@example.com");
        user.setIsEmailVerified(false);

        UpdateUserDto dto = new UpdateUserDto();
        dto.setUsername("newuser");
        dto.setFirstName("New");
        dto.setLastName("User");
        dto.setEmail("new@example.com");
        dto.setIsEmailVerified(true);

        // Act
        mapper.updateEntity(user, dto);

        // Assert
        assertEquals("newuser", user.getUsername());
        assertEquals("New", user.getFirstName());
        assertEquals("new@example.com", user.getEmail());
        assertTrue(user.getIsEmailVerified());
    }

    @Test
    void testUpdateEntityPartialUpdate() {
        // Arrange
        User user = new User();
        user.setUsername("original");
        user.setEmail("original@example.com");
        user.setPiiStripped(false);

        UpdateUserDto dto = new UpdateUserDto();
        dto.setEmail("updated@example.com");

        // Act
        mapper.updateEntity(user, dto);

        // Assert
        assertEquals("original", user.getUsername());
        assertEquals("updated@example.com", user.getEmail());
        assertFalse(user.getPiiStripped());
    }

    @Test
    void testUpdateEntityWithFlags() {
        // Arrange
        User user = new User();

        UpdateUserDto dto = new UpdateUserDto();
        dto.setPiiStripped(true);
        dto.setLegalHold(true);

        // Act
        mapper.updateEntity(user, dto);

        // Assert
        assertTrue(user.getPiiStripped());
        assertTrue(user.getLegalHold());
    }

    @Test
    void testToDtoFullEntity() {
        // Arrange
        User entity = new User();
        entity.setId(10L);
        entity.setUsername("testuser");
        entity.setFirstName("Test");
        entity.setLastName("User");
        entity.setEmail("test@example.com");
        entity.setRole(Role.ADMIN);
        entity.setIsEmailVerified(true);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        UserResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals("10", dto.getId());
        assertEquals("testuser", dto.getUsername());
        assertEquals("Test", dto.getFirstName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("admin", dto.getRole());
        assertTrue(dto.getEmailVerified());
    }

    @Test
    void testToDtoNullEntity() {
        // Act
        UserResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDtoWithNullId() {
        // Arrange
        User entity = new User();
        entity.setId(null);
        entity.setUsername("nouser");
        entity.setEmail("no@example.com");

        // Act
        UserResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getId());
    }

    @Test
    void testToDoDifferentRoles() {
        // Arrange
        Role[] roles = {Role.ADMIN, Role.USER, Role.VIEWER};

        for (Role role : roles) {
            User entity = new User();
            entity.setId(15L);
            entity.setUsername("user");
            entity.setEmail("user@example.com");
            entity.setRole(role);

            // Act
            UserResponseDto dto = mapper.toDto(entity);

            // Assert
            assertEquals(role.getValue(), dto.getRole());
        }
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        User entity = new User();
        entity.setId(16L);
        entity.setUsername("complete");
        entity.setFirstName("Complete");
        entity.setLastName("User");
        entity.setEmail("complete@example.com");
        entity.setRole(Role.ADMIN);
        entity.setIsEmailVerified(true);
        entity.setCreatedAt(LocalDateTime.of(2024, 5, 15, 10, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 5, 15, 11, 30, 0));

        // Act
        UserResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("complete", dto.getUsername());
        assertEquals("Complete", dto.getFirstName());
        assertEquals("User", dto.getLastName());
        assertEquals("complete@example.com", dto.getEmail());
        assertEquals("admin", dto.getRole());
        assertTrue(dto.getEmailVerified());
    }
}

