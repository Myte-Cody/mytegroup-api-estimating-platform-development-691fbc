package com.mytegroup.api.mapper.companylocations;

import com.mytegroup.api.dto.companylocations.CreateCompanyLocationDto;
import com.mytegroup.api.dto.companylocations.UpdateCompanyLocationDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CompanyLocationMapper.
 */
class CompanyLocationMapperTest {

    private CompanyLocationMapper mapper;
    private Organization testOrg;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        mapper = new CompanyLocationMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
        testCompany = new Company();
        testCompany.setId(1L);
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreateCompanyLocationDto dto = new CreateCompanyLocationDto(
                "1",
                "Test Location",
                "EXT-001",
                "America/New_York",
                "location@test.com",
                "+1234567890",
                "123 Main St",
                "Suite 100",
                "New York",
                "NY",
                "10001",
                "US",
                List.of("tag1"),
                "Test notes"
        );

        // When
        CompanyLocation location = mapper.toEntity(dto, testOrg, testCompany);

        // Then
        assertThat(location).isNotNull();
        assertThat(location.getName()).isEqualTo("Test Location");
        assertThat(location.getOrganization()).isEqualTo(testOrg);
        assertThat(location.getCompany()).isEqualTo(testCompany);
        assertThat(location.getTagKeys()).containsExactly("tag1");
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        CompanyLocation location = new CompanyLocation();
        location.setName("Original");
        location.setCity("Original City");

        UpdateCompanyLocationDto dto = new UpdateCompanyLocationDto(
                "Updated",
                null,
                null,
                null,
                null,
                null,
                null,
                "Updated City",
                null,
                null,
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(location, dto);

        // Then
        assertThat(location.getName()).isEqualTo("Updated");
        assertThat(location.getCity()).isEqualTo("Updated City");
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        CompanyLocation location = new CompanyLocation();
        location.setName("Original");
        location.setCity("Original City");

        UpdateCompanyLocationDto dto = new UpdateCompanyLocationDto(
                null,
                null,
                null,
                null,
                null,
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
        mapper.updateEntity(location, dto);

        // Then
        assertThat(location.getName()).isEqualTo("Original");
        assertThat(location.getCity()).isEqualTo("Original City");
    }
}

