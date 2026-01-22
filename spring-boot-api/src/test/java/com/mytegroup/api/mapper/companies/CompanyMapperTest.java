package com.mytegroup.api.mapper.companies;

import com.mytegroup.api.dto.companies.CreateCompanyDto;
import com.mytegroup.api.dto.companies.UpdateCompanyDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CompanyMapper.
 * Tests DTO to Entity mapping, update logic, and null handling.
 */
class CompanyMapperTest {

    private CompanyMapper mapper;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        mapper = new CompanyMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
        testOrg.setName("Test Org");
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreateCompanyDto dto = new CreateCompanyDto(
                "Test Company",
                "EXT-001",
                "https://test.com",
                "test@test.com",
                "+1234567890",
                List.of("type1", "type2"),
                List.of("tag1"),
                4.5,
                "Test notes"
        );

        // When
        Company company = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(company).isNotNull();
        assertThat(company.getName()).isEqualTo("Test Company");
        assertThat(company.getExternalId()).isEqualTo("EXT-001");
        assertThat(company.getWebsite()).isEqualTo("https://test.com");
        assertThat(company.getMainEmail()).isEqualTo("test@test.com");
        assertThat(company.getMainPhone()).isEqualTo("+1234567890");
        assertThat(company.getCompanyTypeKeys()).containsExactly("type1", "type2");
        assertThat(company.getTagKeys()).containsExactly("tag1");
        assertThat(company.getRating()).isEqualTo(4.5);
        assertThat(company.getNotes()).isEqualTo("Test notes");
        assertThat(company.getOrganization()).isEqualTo(testOrg);
    }

    @Test
    void shouldMapCreateDtoWithNullValues() {
        // Given
        CreateCompanyDto dto = new CreateCompanyDto(
                "Test Company",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When
        Company company = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(company).isNotNull();
        assertThat(company.getName()).isEqualTo("Test Company");
        assertThat(company.getExternalId()).isNull();
        assertThat(company.getWebsite()).isNull();
        assertThat(company.getMainEmail()).isNull();
        assertThat(company.getMainPhone()).isNull();
        assertThat(company.getCompanyTypeKeys()).isNull();
        assertThat(company.getTagKeys()).isNull();
        assertThat(company.getRating()).isNull();
        assertThat(company.getNotes()).isNull();
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        Company company = new Company();
        company.setName("Original Name");
        company.setExternalId("ORIG-001");
        company.setWebsite("https://original.com");
        company.setRating(3.0);

        UpdateCompanyDto dto = new UpdateCompanyDto(
                "Updated Name",
                "UPD-001",
                "https://updated.com",
                null,
                null,
                null,
                null,
                4.5,
                null
        );

        // When
        mapper.updateEntity(company, dto);

        // Then
        assertThat(company.getName()).isEqualTo("Updated Name");
        assertThat(company.getExternalId()).isEqualTo("UPD-001");
        assertThat(company.getWebsite()).isEqualTo("https://updated.com");
        assertThat(company.getRating()).isEqualTo(4.5);
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        Company company = new Company();
        company.setName("Original Name");
        company.setExternalId("ORIG-001");
        company.setWebsite("https://original.com");
        company.setMainEmail("original@test.com");
        company.setRating(3.0);

        UpdateCompanyDto dto = new UpdateCompanyDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                4.5,
                null
        );

        // When
        mapper.updateEntity(company, dto);

        // Then
        assertThat(company.getName()).isEqualTo("Original Name");
        assertThat(company.getExternalId()).isEqualTo("ORIG-001");
        assertThat(company.getWebsite()).isEqualTo("https://original.com");
        assertThat(company.getMainEmail()).isEqualTo("original@test.com");
        assertThat(company.getRating()).isEqualTo(4.5);
    }

    @Test
    void shouldUpdateEntityWithPartialValues() {
        // Given
        Company company = new Company();
        company.setName("Original Name");
        company.setExternalId("ORIG-001");
        company.setMainEmail("original@test.com");

        UpdateCompanyDto dto = new UpdateCompanyDto(
                "Updated Name",
                null,
                null,
                "updated@test.com",
                null,
                null,
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(company, dto);

        // Then
        assertThat(company.getName()).isEqualTo("Updated Name");
        assertThat(company.getExternalId()).isEqualTo("ORIG-001"); // Should remain unchanged
        assertThat(company.getMainEmail()).isEqualTo("updated@test.com");
    }
}

