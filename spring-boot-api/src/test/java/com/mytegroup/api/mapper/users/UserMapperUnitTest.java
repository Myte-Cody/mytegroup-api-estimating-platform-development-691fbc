package com.mytegroup.api.mapper.users;

import com.mytegroup.api.dto.users.CreateUserDto;
import com.mytegroup.api.dto.users.UpdateUserDto;
import com.mytegroup.api.entity.people.User;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.common.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperUnitTest {

    private UserMapper userMapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testCreateUserDtoToEntity() {
        // Arrange
        CreateUserDto dto = new CreateUserDto(
            "john_doe",
            "John",
            "Doe",
            "john@example.com",
            "password123",
            Role.ADMIN,
            "1"
        );

        // Act
        User user = userMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(user);
        assertEquals("john_doe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals(organization, user.getOrganization());
    }

    @Test
    void testUpdateUserDtoToEntity() {
        // Arrange
        User user = new User();
        user.setUsername("john_doe");
        user.setFirstName("John");

        UpdateUserDto dto = new UpdateUserDto(
            "Jane",
            "Smith",
            "jane@example.com"
        );

        // Act
        userMapper.updateEntity(user, dto);

        // Assert
        assertEquals("john_doe", user.getUsername());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("jane@example.com", user.getEmail());
    }

    @Test
    void testCreateUserWithNullOptionalFields() {
        // Arrange
        CreateUserDto dto = new CreateUserDto(
            "testuser",
            null,
            null,
            "test@example.com",
            "pass123",
            Role.USER,
            "1"
        );

        // Act
        User user = userMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
    }
}

