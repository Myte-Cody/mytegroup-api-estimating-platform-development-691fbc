package com.mytegroup.api.mapper.persons;

import com.mytegroup.api.dto.response.PersonResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PersonMapperToDtoUnitTest {

    private PersonMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new PersonMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testToDtoWithFullEntity() {
        // Arrange
        Person entity = new Person();
        entity.setId(10L);
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setPrimaryPhoneE164("+12025551234");
        entity.setTitle("Engineer");
        entity.setNotes("Test notes");
        entity.setPiiStripped(false);
        entity.setLegalHold(true);
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        PersonResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("+12025551234", dto.getPrimaryPhoneE164());
        assertEquals("Engineer", dto.getTitle());
        assertEquals("1", dto.getOrgId());
        assertFalse(dto.getPiiStripped());
        assertTrue(dto.getLegalHold());
    }

    @Test
    void testToDtoNullEntity() {
        // Act
        PersonResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDtoWithOnlyFirstName() {
        // Arrange
        Person entity = new Person();
        entity.setId(11L);
        entity.setFirstName("Jane");
        entity.setLastName(null);
        entity.setOrganization(organization);

        // Act
        PersonResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Jane", dto.getFullName());
    }

    @Test
    void testToDtoBuildFullNameCorrectly() {
        // Arrange
        Person entity = new Person();
        entity.setId(12L);
        entity.setFirstName("Robert");
        entity.setLastName("Smith");
        entity.setOrganization(organization);

        // Act
        PersonResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Robert Smith", dto.getFullName());
    }

    @Test
    void testToDtoWithNullNames() {
        // Arrange
        Person entity = new Person();
        entity.setId(13L);
        entity.setFirstName(null);
        entity.setLastName(null);
        entity.setOrganization(organization);

        // Act
        PersonResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getFullName());
    }

    @Test
    void testToDtoMapsAllFields() {
        // Arrange
        Person entity = new Person();
        entity.setId(14L);
        entity.setFirstName("Complete");
        entity.setLastName("Person");
        entity.setPrimaryPhoneE164("+14155552671");
        entity.setTitle("Manager");
        entity.setNotes("Manager notes");
        entity.setPiiStripped(true);
        entity.setLegalHold(false);
        entity.setArchivedAt(LocalDateTime.of(2024, 6, 1, 0, 0, 0));
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 30, 0));

        // Act
        PersonResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Complete Person", dto.getFullName());
        assertTrue(dto.getPiiStripped());
        assertFalse(dto.getLegalHold());
        assertNotNull(dto.getArchivedAt());
    }
}



