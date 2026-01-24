package com.mytegroup.api.mapper.organizations;

import com.mytegroup.api.dto.organizations.CreateOrganizationDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationMapperUnitTest {

    private OrganizationMapper organizationMapper;

    @BeforeEach
    void setUp() {
        organizationMapper = new OrganizationMapper();
    }

    @Test
    void testCreateOrganizationDtoToEntity() {
        // Arrange
        CreateOrganizationDto dto = new CreateOrganizationDto(
            "Acme Corp",
            "acme.com"
        );

        // Act
        Organization org = organizationMapper.toEntity(dto, null, null);

        // Assert
        assertNotNull(org);
        assertEquals("Acme Corp", org.getName());
        assertEquals("acme.com", org.getPrimaryDomain());
    }

    @Test
    void testUpdateOrganizationDtoToEntity() {
        // Arrange
        Organization org = new Organization();
        org.setName("Old Name");

        UpdateOrganizationDto dto = new UpdateOrganizationDto(
            "New Name",
            "new.com"
        );

        // Act
        organizationMapper.updateEntity(org, dto);

        // Assert
        assertEquals("New Name", org.getName());
        assertEquals("new.com", org.getPrimaryDomain());
    }

    @Test
    void testCreateOrganizationWithNullDomain() {
        // Arrange
        CreateOrganizationDto dto = new CreateOrganizationDto(
            "Company Name",
            null
        );

        // Act
        Organization org = organizationMapper.toEntity(dto, null, null);

        // Assert
        assertNotNull(org);
        assertEquals("Company Name", org.getName());
        assertNull(org.getPrimaryDomain());
    }
}

