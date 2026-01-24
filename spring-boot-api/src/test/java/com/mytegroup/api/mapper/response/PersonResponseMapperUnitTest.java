package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.PersonResponseDto;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PersonResponseMapperUnitTest {

    private PersonResponseMapper personResponseMapper;

    @BeforeEach
    void setUp() {
        personResponseMapper = new PersonResponseMapper();
    }

    @Test
    void testPersonEntityToResponseDto() {
        // Arrange
        Organization org = new Organization();
        org.setId(1L);

        Person person = new Person();
        person.setId(50L);
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setFullName("John Doe");
        person.setPrimaryPhoneE164("+1234567890");
        person.setTitle("Senior Manager");
        person.setNotes("Notes about person");
        person.setPiiStripped(false);
        person.setLegalHold(false);
        person.setOrganization(org);
        person.setCreatedAt(LocalDateTime.now());
        person.setUpdatedAt(LocalDateTime.now());

        // Act
        PersonResponseDto dto = personResponseMapper.toDto(person);

        // Assert
        assertNotNull(dto);
        assertEquals(50L, dto.id());
        assertEquals("John", dto.firstName());
        assertEquals("Doe", dto.lastName());
        assertEquals("John Doe", dto.fullName());
        assertEquals("+1234567890", dto.primaryPhoneE164());
        assertEquals("Senior Manager", dto.title());
        assertEquals("Notes about person", dto.notes());
        assertFalse(dto.piiStripped());
        assertEquals(1L, dto.orgId());
    }

    @Test
    void testPersonWithNullValues() {
        // Arrange
        Person person = new Person();
        person.setId(60L);
        person.setFirstName("Jane");
        person.setLastName("Smith");

        // Act
        PersonResponseDto dto = personResponseMapper.toDto(person);

        // Assert
        assertNotNull(dto);
        assertEquals(60L, dto.id());
        assertEquals("Jane", dto.firstName());
        assertEquals("Smith", dto.lastName());
        assertNull(dto.fullName());
        assertNull(dto.title());
    }

    @Test
    void testNullEntityReturnsNull() {
        // Act
        PersonResponseDto dto = personResponseMapper.toDto(null);

        // Assert
        assertNull(dto);
    }
}

