package com.mytegroup.api.mapper.persons;

import com.mytegroup.api.dto.persons.CreatePersonDto;
import com.mytegroup.api.dto.persons.UpdatePersonDto;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PersonMapperUnitTest {

    private PersonMapper personMapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        personMapper = new PersonMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testCreatePersonDtoToEntity() {
        // Arrange
        LocalDate dob = LocalDate.of(1990, 1, 15);
        CreatePersonDto dto = new CreatePersonDto(
            null,
            "John",
            "Doe",
            LocalDate.of(1990, 1, 15),
            "john@example.com",
            "+1234567890",
            null,
            Arrays.asList("TAG1"),
            Arrays.asList("SKILL1"),
            "DEPT1",
            null,
            null,
            null,
            null,
            "EMP001",
            null,
            null,
            null,
            "Senior",
            "Test notes",
            null
        );

        // Act
        Person person = personMapper.toEntity(dto, organization, null, null, null, null);

        // Assert
        assertNotNull(person);
        assertEquals("John", person.getFirstName());
        assertEquals("Doe", person.getLastName());
        assertEquals(dob, person.getDateOfBirth());
        assertEquals("john@example.com", person.getPrimaryEmail());
        assertEquals("+1234567890", person.getPrimaryPhoneE164());
        assertEquals("DEPT1", person.getDepartmentKey());
        assertEquals("EMP001", person.getIronworkerNumber());
        assertEquals("Senior", person.getTitle());
        assertEquals("Test notes", person.getNotes());
        assertEquals(organization, person.getOrganization());
    }

    @Test
    void testUpdatePersonDtoToEntity() {
        // Arrange
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");

        UpdatePersonDto dto = new UpdatePersonDto(
            null,
            "Jane",
            "Smith",
            null,
            "jane@example.com",
            "+9876543210",
            null,
            Arrays.asList("TAG2"),
            Arrays.asList("SKILL2"),
            "DEPT2",
            null,
            null,
            null,
            null,
            "EMP002",
            null,
            null,
            null,
            "Lead",
            "Updated notes",
            null
        );

        // Act
        personMapper.updateEntity(person, dto, null, null, null, null);

        // Assert
        assertEquals("Jane", person.getFirstName());
        assertEquals("Smith", person.getLastName());
        assertEquals("jane@example.com", person.getPrimaryEmail());
        assertEquals("+9876543210", person.getPrimaryPhoneE164());
        assertEquals("DEPT2", person.getDepartmentKey());
        assertEquals("EMP002", person.getIronworkerNumber());
        assertEquals("Lead", person.getTitle());
        assertEquals("Updated notes", person.getNotes());
    }

    @Test
    void testPersonDtoWithMinimalData() {
        // Arrange
        CreatePersonDto dto = new CreatePersonDto(
            null, "John", "Doe", null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null
        );

        // Act
        Person person = personMapper.toEntity(dto, organization, null, null, null, null);

        // Assert
        assertNotNull(person);
        assertEquals("John", person.getFirstName());
        assertEquals("Doe", person.getLastName());
        assertEquals(organization, person.getOrganization());
    }
}

