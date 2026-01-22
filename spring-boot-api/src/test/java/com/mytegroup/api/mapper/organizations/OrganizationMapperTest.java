package com.mytegroup.api.mapper.organizations;

import com.mytegroup.api.dto.organizations.CreateOrganizationDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.organization.DataResidency;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrganizationMapper.
 */
class OrganizationMapperTest {

    private OrganizationMapper mapper;
    private User ownerUser;
    private User createdByUser;

    @BeforeEach
    void setUp() {
        mapper = new OrganizationMapper();
        ownerUser = new User();
        ownerUser.setId(1L);
        createdByUser = new User();
        createdByUser.setId(2L);
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreateOrganizationDto dto = new CreateOrganizationDto(
                "Test Org",
                Map.of("key", "value"),
                "postgresql://localhost/test",
                "postgresql://localhost/test",
                "testdb",
                "test.com",
                true,
                DatastoreType.DEDICATED,
                DataResidency.US,
                false,
                false
        );

        // When
        Organization org = mapper.toEntity(dto, ownerUser, createdByUser);

        // Then
        assertThat(org).isNotNull();
        assertThat(org.getName()).isEqualTo("Test Org");
        assertThat(org.getOwnerUser()).isEqualTo(ownerUser);
        assertThat(org.getCreatedByUser()).isEqualTo(createdByUser);
        assertThat(org.getUseDedicatedDb()).isTrue();
        assertThat(org.getDatastoreType()).isEqualTo(DatastoreType.DEDICATED);
    }

    @Test
    void shouldMapCreateDtoWithDefaults() {
        // Given
        CreateOrganizationDto dto = new CreateOrganizationDto(
                "Test Org",
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
        Organization org = mapper.toEntity(dto, ownerUser, createdByUser);

        // Then
        assertThat(org.getUseDedicatedDb()).isFalse();
        assertThat(org.getDatastoreType()).isEqualTo(DatastoreType.SHARED);
        assertThat(org.getDataResidency()).isEqualTo(DataResidency.SHARED);
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        Organization org = new Organization();
        org.setName("Original Name");
        org.setMetadata(Map.of("original", "value"));

        UpdateOrganizationDto dto = new UpdateOrganizationDto(
                "Updated Name",
                Map.of("updated", "value")
        );

        // When
        mapper.updateEntity(org, dto);

        // Then
        assertThat(org.getName()).isEqualTo("Updated Name");
        assertThat(org.getMetadata()).containsEntry("updated", "value");
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        Organization org = new Organization();
        org.setName("Original Name");
        org.setMetadata(Map.of("original", "value"));

        UpdateOrganizationDto dto = new UpdateOrganizationDto(
                null,
                null
        );

        // When
        mapper.updateEntity(org, dto);

        // Then
        assertThat(org.getName()).isEqualTo("Original Name");
        assertThat(org.getMetadata()).containsEntry("original", "value");
    }
}

