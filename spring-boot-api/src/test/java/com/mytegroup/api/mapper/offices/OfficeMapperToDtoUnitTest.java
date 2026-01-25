package com.mytegroup.api.mapper.offices;

import com.mytegroup.api.dto.response.OfficeResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OfficeMapperToDtoUnitTest {

    private OfficeMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new OfficeMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testToDtoWithFullEntity() {
        // Arrange
        Office entity = new Office();
        entity.setId(10L);
        entity.setName("Main Office");
        entity.setAddress("123 Main St");
        entity.setOrganization(organization);
        entity.setArchivedAt(null);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        OfficeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Main Office", dto.getName());
        assertEquals("123 Main St", dto.getAddress());
        assertEquals("1", dto.getOrgId());
        assertNull(dto.getArchivedAt());
    }

    @Test
    void testToDtoWithNullEntity() {
        // Act
        OfficeResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDtoWithoutOrganization() {
        // Arrange
        Office entity = new Office();
        entity.setId(11L);
        entity.setName("Office Without Org");
        entity.setAddress("456 St");
        entity.setOrganization(null);

        // Act
        OfficeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getOrgId());
        assertEquals("Office Without Org", dto.getName());
    }

    @Test
    void testToDtoWithArchivedDate() {
        // Arrange
        Office entity = new Office();
        entity.setId(12L);
        entity.setName("Archived Office");
        entity.setAddress("789 Ave");
        entity.setOrganization(organization);
        entity.setArchivedAt(LocalDateTime.of(2024, 6, 1, 0, 0, 0));

        // Act
        OfficeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto.getArchivedAt());
        assertEquals(LocalDateTime.of(2024, 6, 1, 0, 0, 0), dto.getArchivedAt());
    }

    @Test
    void testToDtoMapsAllFields() {
        // Arrange
        Office entity = new Office();
        entity.setId(13L);
        entity.setName("Complete Office");
        entity.setAddress("101 Park Ave");
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 30, 0));

        // Act
        OfficeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Complete Office", dto.getName());
        assertEquals("101 Park Ave", dto.getAddress());
        assertEquals("1", dto.getOrgId());
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 15, 11, 30, 0), dto.getUpdatedAt());
    }

    @Test
    void testToDtoWithNullAddress() {
        // Arrange
        Office entity = new Office();
        entity.setId(14L);
        entity.setName("No Address Office");
        entity.setAddress(null);
        entity.setOrganization(organization);

        // Act
        OfficeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getAddress());
        assertEquals("No Address Office", dto.getName());
    }
}

