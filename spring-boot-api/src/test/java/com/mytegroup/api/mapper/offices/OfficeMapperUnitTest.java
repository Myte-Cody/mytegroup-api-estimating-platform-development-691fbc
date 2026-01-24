package com.mytegroup.api.mapper.offices;

import com.mytegroup.api.dto.offices.CreateOfficeDto;
import com.mytegroup.api.dto.offices.UpdateOfficeDto;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class OfficeMapperUnitTest {

    private OfficeMapper officeMapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        officeMapper = new OfficeMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testCreateOfficeDtoToEntity() {
        // Arrange
        CreateOfficeDto dto = new CreateOfficeDto(
            "NYC",
            "New York Office",
            "UTC",
            "NYC_OFFICE",
            null
        );

        // Act
        Office office = officeMapper.toEntity(dto, organization, null);

        // Assert
        assertNotNull(office);
        assertEquals("NYC", office.getCode());
        assertEquals("New York Office", office.getName());
        assertEquals("UTC", office.getTimezone());
        assertEquals("NYC_OFFICE", office.getOrgLocationTypeKey());
        assertEquals(organization, office.getOrganization());
    }

    @Test
    void testUpdateOfficeDtoToEntity() {
        // Arrange
        Office office = new Office();
        office.setCode("NYC");
        office.setName("Old Name");

        UpdateOfficeDto dto = new UpdateOfficeDto(
            "Updated Office",
            "EST",
            "NEW_TYPE",
            null
        );

        // Act
        officeMapper.updateEntity(office, dto, null);

        // Assert
        assertEquals("NYC", office.getCode());
        assertEquals("Updated Office", office.getName());
        assertEquals("EST", office.getTimezone());
        assertEquals("NEW_TYPE", office.getOrgLocationTypeKey());
    }
}

