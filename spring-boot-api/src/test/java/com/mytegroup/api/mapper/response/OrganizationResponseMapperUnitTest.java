package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.OrganizationResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.User;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationResponseMapperUnitTest {

    private OrganizationResponseMapper organizationResponseMapper;

    @BeforeEach
    void setUp() {
        organizationResponseMapper = new OrganizationResponseMapper();
    }

    @Test
    void testOrganizationEntityToResponseDto() {
        // Arrange
        User owner = new User();
        owner.setId("owner1");

        Organization org = new Organization();
        org.setId(1L);
        org.setName("Acme Corp");
        org.setPrimaryDomain("acme.com");
        org.setDatastoreType(DatastoreType.SQL_POSTGRES);
        org.setDatabaseUri("postgres://localhost:5432");
        org.setDatabaseName("acme_db");
        org.setOwnerUser(owner);
        org.setPiiStripped(false);
        org.setLegalHold(false);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());

        // Act
        OrganizationResponseDto dto = organizationResponseMapper.toDto(org);

        // Assert
        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("Acme Corp", dto.name());
        assertEquals("acme.com", dto.primaryDomain());
        assertEquals("SQL_POSTGRES", dto.datastoreType());
        assertEquals("owner1", dto.ownerId());
        assertFalse(dto.piiStripped());
        assertFalse(dto.legalHold());
        
        Map<String, Object> config = dto.datastoreConfig();
        assertNotNull(config);
        assertEquals("postgres://localhost:5432", config.get("databaseUri"));
        assertEquals("acme_db", config.get("databaseName"));
    }

    @Test
    void testOrganizationWithoutOwner() {
        // Arrange
        Organization org = new Organization();
        org.setId(2L);
        org.setName("Test Org");
        org.setOwnerUser(null);

        // Act
        OrganizationResponseDto dto = organizationResponseMapper.toDto(org);

        // Assert
        assertNotNull(dto);
        assertEquals(2L, dto.id());
        assertEquals("Test Org", dto.name());
        assertNull(dto.ownerId());
    }

    @Test
    void testNullEntityReturnsNull() {
        // Act
        OrganizationResponseDto dto = organizationResponseMapper.toDto(null);

        // Assert
        assertNull(dto);
    }
}

