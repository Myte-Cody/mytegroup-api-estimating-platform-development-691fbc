package com.mytegroup.api.mapper.companies;

import com.mytegroup.api.dto.companies.CreateCompanyDto;
import com.mytegroup.api.dto.companies.UpdateCompanyDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompanyMapperUnitTest {

    private CompanyMapper companyMapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        companyMapper = new CompanyMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");
    }

    @Test
    void testCreateCompanyDtoToEntity() {
        // Arrange
        CreateCompanyDto dto = new CreateCompanyDto(
            "Test Company",
            "EXT123",
            "test.com",
            "contact@test.com",
            "555-0100",
            Arrays.asList("TYPE1", "TYPE2"),
            Arrays.asList("TAG1", "TAG2"),
            4.5,
            "Notes here"
        );

        // Act
        Company company = companyMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(company);
        assertEquals("Test Company", company.getName());
        assertEquals("test.com", company.getWebsite());
        assertEquals("EXT123", company.getExternalId());
        assertEquals(organization, company.getOrganization());
        assertEquals(2, company.getCompanyTypeKeys().size());
        assertEquals(2, company.getTagKeys().size());
    }

    @Test
    void testUpdateCompanyDtoToEntity() {
        // Arrange
        Company company = new Company();
        company.setName("Old Name");
        company.setWebsite("old.com");

        UpdateCompanyDto dto = new UpdateCompanyDto(
            "New Name",
            "NEW123",
            "new.com",
            "new@company.com",
            "555-0001",
            Arrays.asList("NEWTYPE"),
            Arrays.asList("NEWTAG"),
            3.5,
            "Updated notes"
        );

        // Act
        companyMapper.updateEntity(company, dto);

        // Assert
        assertEquals("New Name", company.getName());
        assertEquals("new.com", company.getWebsite());
        assertEquals("NEW123", company.getExternalId());
        assertEquals(1, company.getCompanyTypeKeys().size());
        assertEquals(1, company.getTagKeys().size());
    }

    @Test
    void testCreateCompanyDtoWithNullValues() {
        // Arrange
        CreateCompanyDto dto = new CreateCompanyDto(
            "Company Name",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        // Act
        Company company = companyMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(company);
        assertEquals("Company Name", company.getName());
        assertNull(company.getWebsite());
        assertNull(company.getExternalId());
    }
}

