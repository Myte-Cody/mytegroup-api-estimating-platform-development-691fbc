package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.ContactResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.people.ContactPersonType;
import com.mytegroup.api.entity.people.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ContactResponseMapperUnitTest {

    private ContactResponseMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new ContactResponseMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testToDtoFullEntity() {
        // Arrange
        Contact entity = new Contact();
        entity.setId(10L);
        entity.setName("John Contact");
        entity.setFirstName("John");
        entity.setLastName("Contact");
        entity.setEmail("john@example.com");
        entity.setPhone("+12025551234");
        entity.setPersonType(ContactPersonType.STAFF);
        entity.setNotes("Test notes");
        entity.setIronworkerNumber("IW123");
        entity.setCompany("Test Company");
        entity.setPiiStripped(false);
        entity.setLegalHold(false);
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        ContactResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("John Contact", dto.getName());
        assertEquals("John", dto.getFirstName());
        assertEquals("staff", dto.getPersonType());
        assertEquals("1", dto.getOrgId());
    }

    @Test
    void testToDtoNullEntity() {
        // Act
        ContactResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDoDifferentPersonTypes() {
        // Arrange
        ContactPersonType[] types = {
                ContactPersonType.STAFF,
                ContactPersonType.IRONWORKER,
                ContactPersonType.EXTERNAL
        };

        for (ContactPersonType type : types) {
            Contact entity = new Contact();
            entity.setId(11L);
            entity.setName("Test");
            entity.setPersonType(type);
            entity.setOrganization(organization);

            // Act
            ContactResponseDto dto = mapper.toDto(entity);

            // Assert
            assertEquals(type.getValue(), dto.getPersonType());
        }
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        Contact entity = new Contact();
        entity.setId(12L);
        entity.setName("Complete Contact");
        entity.setFirstName("Complete");
        entity.setLastName("Contact");
        entity.setEmail("complete@example.com");
        entity.setPhone("+14155552671");
        entity.setPersonType(ContactPersonType.IRONWORKER);
        entity.setNotes("Complete notes");
        entity.setIronworkerNumber("IW999");
        entity.setCompany("Complete Company");
        entity.setPiiStripped(true);
        entity.setLegalHold(true);
        entity.setArchivedAt(LocalDateTime.of(2024, 6, 1, 0, 0, 0));
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 0, 0));

        // Act
        ContactResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Complete Contact", dto.getName());
        assertEquals("Complete", dto.getFirstName());
        assertEquals("Contact", dto.getLastName());
        assertEquals("complete@example.com", dto.getEmail());
        assertEquals("ironworker", dto.getPersonType());
        assertTrue(dto.getPiiStripped());
        assertTrue(dto.getLegalHold());
    }

    @Test
    void testToDoBuildsMapsWithNullOptionalFields() {
        // Arrange
        Contact entity = new Contact();
        entity.setId(13L);
        entity.setName("Minimal Contact");
        entity.setFirstName(null);
        entity.setLastName(null);
        entity.setEmail(null);
        entity.setPhone(null);
        entity.setPersonType(null);
        entity.setNotes(null);
        entity.setIronworkerNumber(null);
        entity.setCompany(null);
        entity.setOrganization(organization);

        // Act
        ContactResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Minimal Contact", dto.getName());
        assertNull(dto.getFirstName());
        assertNull(dto.getPersonType());
        assertNull(dto.getIronworkerNumber());
        assertEquals("1", dto.getOrgId());
    }

    @Test
    void testToDoBuildsMapsWithNullOrganization() {
        // Arrange
        Contact entity = new Contact();
        entity.setId(14L);
        entity.setName("No Org Contact");
        entity.setOrganization(null);

        // Act
        ContactResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getOrgId());
    }

    @Test
    void testToDoBuildsMapsDateFields() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 15, 11, 30, 0);
        LocalDateTime archived = LocalDateTime.of(2024, 6, 15, 0, 0, 0);

        Contact entity = new Contact();
        entity.setId(15L);
        entity.setName("Dated Contact");
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);
        entity.setArchivedAt(archived);
        entity.setOrganization(organization);

        // Act
        ContactResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
        assertEquals(archived, dto.getArchivedAt());
    }
}

