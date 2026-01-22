package com.mytegroup.api.mapper.offices;

import com.mytegroup.api.dto.offices.CreateOfficeDto;
import com.mytegroup.api.dto.offices.UpdateOfficeDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OfficeMapper.
 */
class OfficeMapperTest {

    private OfficeMapper mapper;
    private Organization testOrg;
    private Office parentOffice;

    @BeforeEach
    void setUp() {
        mapper = new OfficeMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
        parentOffice = new Office();
        parentOffice.setId(1L);
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreateOfficeDto dto = new CreateOfficeDto(
                "Test Office",
                "123 Main St",
                "Description",
                "America/New_York",
                "HEADQUARTERS",
                List.of("tag1"),
                1,
                1L
        );

        // When
        Office office = mapper.toEntity(dto, testOrg, parentOffice);

        // Then
        assertThat(office).isNotNull();
        assertThat(office.getName()).isEqualTo("Test Office");
        assertThat(office.getOrganization()).isEqualTo(testOrg);
        assertThat(office.getParent()).isEqualTo(parentOffice);
        assertThat(office.getTagKeys()).containsExactly("tag1");
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        Office office = new Office();
        office.setName("Original");
        office.setAddress("Original Address");

        UpdateOfficeDto dto = new UpdateOfficeDto(
                "Updated",
                "Updated Address",
                null,
                null,
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(office, dto, null);

        // Then
        assertThat(office.getName()).isEqualTo("Updated");
        assertThat(office.getAddress()).isEqualTo("Updated Address");
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        Office office = new Office();
        office.setName("Original");
        office.setAddress("Original Address");

        UpdateOfficeDto dto = new UpdateOfficeDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(office, dto, null);

        // Then
        assertThat(office.getName()).isEqualTo("Original");
        assertThat(office.getAddress()).isEqualTo("Original Address");
    }
}

