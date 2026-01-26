package com.mytegroup.api.mapper.response;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.response.InviteResponseDto;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.people.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InviteResponseMapperUnitTest {

    private InviteResponseMapper mapper;
    private Person person;

    @BeforeEach
    void setUp() {
        mapper = new InviteResponseMapper();
        person = new Person();
        person.setId(1L);
        person.setFirstName("John");
    }

    @Test
    void testToDotFullEntity() {
        // Arrange
        Invite entity = new Invite();
        entity.setId(10L);
        entity.setPerson(person);
        entity.setRole(Role.ADMIN);
        entity.setTokenExpires(LocalDateTime.of(2024, 1, 15, 23, 59, 59));
        entity.setAcceptedAt(LocalDateTime.of(2024, 1, 14, 10, 0, 0));
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));

        // Act
        InviteResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("1", dto.getPersonId());
        assertEquals("admin", dto.getRole());
        assertNotNull(dto.getTokenExpires());
        assertNotNull(dto.getAcceptedAt());
    }

    @Test
    void testToDotNullEntity() {
        // Act
        InviteResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDoBuildsMapsWithNullPerson() {
        // Arrange
        Invite entity = new Invite();
        entity.setId(11L);
        entity.setPerson(null);
        entity.setRole(Role.USER);

        // Act
        InviteResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getPersonId());
        assertEquals("user", dto.getRole());
    }

    @Test
    void testToDoBuildsMapsWithDifferentRoles() {
        // Arrange
        Role[] roles = {Role.ADMIN, Role.USER, Role.VIEWER};

        for (Role role : roles) {
            Invite entity = new Invite();
            entity.setId(12L);
            entity.setPerson(person);
            entity.setRole(role);

            // Act
            InviteResponseDto dto = mapper.toDto(entity);

            // Assert
            assertEquals(role.getValue(), dto.getRole());
        }
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        LocalDateTime expires = LocalDateTime.of(2024, 2, 1, 0, 0, 0);
        LocalDateTime accepted = LocalDateTime.of(2024, 1, 20, 15, 30, 0);
        LocalDateTime created = LocalDateTime.of(2024, 1, 15, 10, 0, 0);

        Invite entity = new Invite();
        entity.setId(13L);
        entity.setPerson(person);
        entity.setRole(Role.VIEWER);
        entity.setTokenExpires(expires);
        entity.setAcceptedAt(accepted);
        entity.setCreatedAt(created);

        // Act
        InviteResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("1", dto.getPersonId());
        assertEquals("viewer", dto.getRole());
        assertEquals(expires, dto.getTokenExpires());
        assertEquals(accepted, dto.getAcceptedAt());
        assertEquals(created, dto.getCreatedAt());
    }

    @Test
    void testToDoBuildsMapsWithNullRole() {
        // Arrange
        Invite entity = new Invite();
        entity.setId(14L);
        entity.setPerson(person);
        entity.setRole(null);

        // Act
        InviteResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getRole());
    }

    @Test
    void testToDoBuildsMapsWithNullDates() {
        // Arrange
        Invite entity = new Invite();
        entity.setId(15L);
        entity.setPerson(person);
        entity.setRole(Role.USER);
        entity.setTokenExpires(null);
        entity.setAcceptedAt(null);

        // Act
        InviteResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getTokenExpires());
        assertNull(dto.getAcceptedAt());
    }
}



