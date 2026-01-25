package com.mytegroup.api.mapper.organizations;

import com.mytegroup.api.dto.response.OrganizationResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationMapperToDtoUnitTest {

    private OrganizationMapper mapper;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        mapper = new OrganizationMapper();
        ownerUser = new User();
        ownerUser.setId(1L);
        ownerUser.setUsername("owner");
    }

    @Test
    void testToDtoWithFullEntity() {
        // Arrange
        Organization entity = new Organization();
        entity.setId(10L);
        entity.setName("Test Organization");
        entity.setPrimaryDomain("org.example.com");
        entity.setDatastoreType(DatastoreType.DEDICATED);
        entity.setDatabaseUri("postgresql://localhost/db");
        entity.setDatabaseName("testdb");
        entity.setOwnerUser(ownerUser);
        entity.setPiiStripped(true);
        entity.setLegalHold(false);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        OrganizationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Test Organization", dto.getName());
        assertEquals("org.example.com", dto.getPrimaryDomain());
        assertEquals("dedicated", dto.getDatastoreType());
        assertEquals("1", dto.getOwnerId());
        assertTrue(dto.getPiiStripped());
        assertFalse(dto.getLegalHold());
        assertNotNull(dto.getDatastoreConfig());
    }

    @Test
    void testToDtoNullEntity() {
        // Act
        OrganizationResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDoBuildDatastoreConfig() {
        // Arrange
        Organization entity = new Organization();
        entity.setId(11L);
        entity.setName("Organization");
        entity.setDatabaseUri("postgresql://prod/db");
        entity.setDatabaseName("production");
        entity.setDatastoreType(DatastoreType.SHARED);
        entity.setOwnerUser(null);

        // Act
        OrganizationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto.getDatastoreConfig());
        assertEquals("postgresql://prod/db", dto.getDatastoreConfig().get("databaseUri"));
        assertEquals("production", dto.getDatastoreConfig().get("databaseName"));
    }

    @Test
    void testToDtoWithoutOwner() {
        // Arrange
        Organization entity = new Organization();
        entity.setId(12L);
        entity.setName("No Owner Org");
        entity.setDatastoreType(DatastoreType.SHARED);
        entity.setOwnerUser(null);

        // Act
        OrganizationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getOwnerId());
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        Organization entity = new Organization();
        entity.setId(13L);
        entity.setName("Complete Organization");
        entity.setPrimaryDomain("complete.example.com");
        entity.setDatastoreType(DatastoreType.DEDICATED);
        entity.setDatabaseUri("postgresql://complete/db");
        entity.setDatabaseName("complete_db");
        entity.setOwnerUser(ownerUser);
        entity.setPiiStripped(false);
        entity.setLegalHold(true);
        entity.setArchivedAt(LocalDateTime.of(2024, 6, 1, 0, 0, 0));
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 30, 0));

        // Act
        OrganizationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Complete Organization", dto.getName());
        assertEquals("complete.example.com", dto.getPrimaryDomain());
        assertEquals("dedicated", dto.getDatastoreType());
        assertEquals("1", dto.getOwnerId());
        assertFalse(dto.getPiiStripped());
        assertTrue(dto.getLegalHold());
    }
}


