package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.CompanyResponseDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CompanyResponseMapperUnitTest {

    private CompanyResponseMapper companyResponseMapper;

    @BeforeEach
    void setUp() {
        companyResponseMapper = new CompanyResponseMapper();
    }

    @Test
    void testCompanyEntityToResponseDto() {
        // Arrange
        Organization org = new Organization();
        org.setId(1L);

        Company company = new Company();
        company.setId(100L);
        company.setName("Acme Corp");
        company.setNormalizedName("acme-corp");
        company.setExternalId("EXT123");
        company.setWebsite("acme.com");
        company.setMainEmail("contact@acme.com");
        company.setMainPhone("555-0100");
        company.setCompanyTypeKeys(Arrays.asList("TYPE1"));
        company.setTagKeys(Arrays.asList("TAG1"));
        company.setRating("5");
        company.setNotes("Good company");
        company.setPiiStripped(false);
        company.setLegalHold(false);
        company.setOrganization(org);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());

        // Act
        CompanyResponseDto dto = companyResponseMapper.toDto(company);

        // Assert
        assertNotNull(dto);
        assertEquals(100L, dto.id());
        assertEquals("Acme Corp", dto.name());
        assertEquals("acme-corp", dto.normalizedName());
        assertEquals("EXT123", dto.externalId());
        assertEquals("acme.com", dto.website());
        assertEquals(1L, dto.orgId());
        assertFalse(dto.piiStripped());
        assertFalse(dto.legalHold());
    }

    @Test
    void testCompanyEntityWithNullOrganization() {
        // Arrange
        Company company = new Company();
        company.setId(200L);
        company.setName("Test Company");
        company.setOrganization(null);

        // Act
        CompanyResponseDto dto = companyResponseMapper.toDto(company);

        // Assert
        assertNotNull(dto);
        assertEquals(200L, dto.id());
        assertEquals("Test Company", dto.name());
        assertNull(dto.orgId());
    }

    @Test
    void testNullEntityReturnsNull() {
        // Act
        CompanyResponseDto dto = companyResponseMapper.toDto(null);

        // Assert
        assertNull(dto);
    }
}

