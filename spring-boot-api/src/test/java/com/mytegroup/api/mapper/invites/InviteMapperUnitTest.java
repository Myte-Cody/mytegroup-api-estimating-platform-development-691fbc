package com.mytegroup.api.mapper.invites;

import com.mytegroup.api.dto.invites.CreateInviteDto;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InviteMapperUnitTest {

    private InviteMapper mapper;
    private Organization organization;
    private Person person;

    @BeforeEach
    void setUp() {
        mapper = new InviteMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Organization");
        
        person = new Person();
        person.setId(1L);
        person.setFirstName("John");
        person.setLastName("Doe");
    }

    @Test
    void testCreateInviteDtoToEntity() {
        // Arrange
        CreateInviteDto dto = new CreateInviteDto();
        dto.setRole(Role.USER);
        dto.setExpiresInHours(24);

        // Act
        Invite invite = mapper.toEntity(dto, organization, person);

        // Assert
        assertNotNull(invite);
        assertEquals(organization, invite.getOrganization());
        assertEquals(person, invite.getPerson());
        assertEquals(Role.USER, invite.getRole());
        assertNotNull(invite.getTokenExpires());
        assertTrue(invite.getTokenExpires().isAfter(LocalDateTime.now()));
        assertTrue(invite.getTokenExpires().isBefore(LocalDateTime.now().plusHours(25)));
    }

    @Test
    void testCreateInviteDtoToEntityWithNullExpiresInHours() {
        // Arrange
        CreateInviteDto dto = new CreateInviteDto();
        dto.setRole(Role.ADMIN);
        dto.setExpiresInHours(null);

        // Act
        Invite invite = mapper.toEntity(dto, organization, person);

        // Assert
        assertNotNull(invite);
        assertEquals(Role.ADMIN, invite.getRole());
        assertNotNull(invite.getTokenExpires());
        // Should default to 7 days from now
        assertTrue(invite.getTokenExpires().isAfter(LocalDateTime.now().plusDays(6)));
        assertTrue(invite.getTokenExpires().isBefore(LocalDateTime.now().plusDays(8)));
    }

    @Test
    void testCreateInviteDtoToEntityWithViewer() {
        // Arrange
        CreateInviteDto dto = new CreateInviteDto();
        dto.setRole(Role.VIEWER);
        dto.setExpiresInHours(48);

        // Act
        Invite invite = mapper.toEntity(dto, organization, person);

        // Assert
        assertEquals(Role.VIEWER, invite.getRole());
        assertNotNull(invite.getTokenExpires());
    }

    @Test
    void testCreateInviteDtoToEntitySetsOrganization() {
        // Arrange
        CreateInviteDto dto = new CreateInviteDto();
        dto.setRole(Role.USER);

        // Act
        Invite invite = mapper.toEntity(dto, organization, person);

        // Assert
        assertEquals(organization, invite.getOrganization());
    }

    @Test
    void testCreateInviteDtoToEntitySetsPerson() {
        // Arrange
        CreateInviteDto dto = new CreateInviteDto();
        dto.setRole(Role.USER);

        // Act
        Invite invite = mapper.toEntity(dto, organization, person);

        // Assert
        assertEquals(person, invite.getPerson());
    }

    @Test
    void testCreateInviteDtoToEntityWithDifferentHours() {
        // Arrange
        Integer[] hoursValues = {1, 12, 24, 72, 168};

        for (Integer hours : hoursValues) {
            CreateInviteDto dto = new CreateInviteDto();
            dto.setRole(Role.USER);
            dto.setExpiresInHours(hours);

            // Act
            Invite invite = mapper.toEntity(dto, organization, person);

            // Assert
            assertNotNull(invite.getTokenExpires());
            LocalDateTime expectedMin = LocalDateTime.now().plusHours(hours).minusMinutes(1);
            LocalDateTime expectedMax = LocalDateTime.now().plusHours(hours).plusMinutes(1);
            assertTrue(invite.getTokenExpires().isAfter(expectedMin));
            assertTrue(invite.getTokenExpires().isBefore(expectedMax));
        }
    }

    @Test
    void testCreateInviteDtoToEntityWithDifferentRoles() {
        // Arrange
        Role[] roles = {Role.ADMIN, Role.USER, Role.VIEWER};

        for (Role role : roles) {
            CreateInviteDto dto = new CreateInviteDto();
            dto.setRole(role);
            dto.setExpiresInHours(24);

            // Act
            Invite invite = mapper.toEntity(dto, organization, person);

            // Assert
            assertEquals(role, invite.getRole());
        }
    }

    @Test
    void testCreateInviteDtoToEntityMapsAllFields() {
        // Arrange
        CreateInviteDto dto = new CreateInviteDto();
        dto.setRole(Role.ADMIN);
        dto.setExpiresInHours(72);

        // Act
        Invite invite = mapper.toEntity(dto, organization, person);

        // Assert
        assertEquals(organization, invite.getOrganization());
        assertEquals(person, invite.getPerson());
        assertEquals(Role.ADMIN, invite.getRole());
        assertNotNull(invite.getTokenExpires());
    }

    @Test
    void testCreateInviteDtoToEntityWithZeroHours() {
        // Arrange
        CreateInviteDto dto = new CreateInviteDto();
        dto.setRole(Role.USER);
        dto.setExpiresInHours(0);

        // Act
        Invite invite = mapper.toEntity(dto, organization, person);

        // Assert
        assertNotNull(invite.getTokenExpires());
        LocalDateTime now = LocalDateTime.now();
        assertTrue(invite.getTokenExpires().isAfter(now.minusMinutes(1)));
        assertTrue(invite.getTokenExpires().isBefore(now.plusMinutes(1)));
    }

    @Test
    void testCreateInviteDtoToEntityWithLargeHours() {
        // Arrange
        CreateInviteDto dto = new CreateInviteDto();
        dto.setRole(Role.USER);
        dto.setExpiresInHours(1000);

        // Act
        Invite invite = mapper.toEntity(dto, organization, person);

        // Assert
        assertNotNull(invite.getTokenExpires());
        LocalDateTime expectedMin = LocalDateTime.now().plusHours(1000).minusMinutes(1);
        LocalDateTime expectedMax = LocalDateTime.now().plusHours(1000).plusMinutes(1);
        assertTrue(invite.getTokenExpires().isAfter(expectedMin));
        assertTrue(invite.getTokenExpires().isBefore(expectedMax));
    }
}


