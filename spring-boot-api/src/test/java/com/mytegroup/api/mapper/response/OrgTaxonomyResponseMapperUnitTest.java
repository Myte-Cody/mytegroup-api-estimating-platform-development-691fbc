package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.OrgTaxonomyResponseDto;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrgTaxonomyResponseMapperUnitTest {

    private OrgTaxonomyResponseMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new OrgTaxonomyResponseMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Organization");
    }

    @Test
    void testOrgTaxonomyToDto() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 10, 10, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 15, 30, 0);

        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setId(1L);
        taxonomy.setNamespace("company_types");
        taxonomy.setOrganization(organization);
        taxonomy.setCreatedAt(createdAt);
        taxonomy.setUpdatedAt(updatedAt);

        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

        // Assert
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("company_types", dto.getNamespace());
        assertEquals("1", dto.getOrgId());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    void testOrgTaxonomyToDtoWithNullOrganization() {
        // Arrange
        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setId(2L);
        taxonomy.setNamespace("tags");
        taxonomy.setOrganization(null);
        taxonomy.setCreatedAt(LocalDateTime.now());

        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getOrgId());
        assertEquals("tags", dto.getNamespace());
    }

    @Test
    void testOrgTaxonomyToDtoNull() {
        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testOrgTaxonomyToDtoMapsAllFields() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 8, 0, 0);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 20, 16, 45, 0);

        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setId(99L);
        taxonomy.setNamespace("complete_namespace");
        taxonomy.setOrganization(organization);
        taxonomy.setCreatedAt(created);
        taxonomy.setUpdatedAt(updated);

        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

        // Assert
        assertEquals(99L, dto.getId());
        assertEquals("complete_namespace", dto.getNamespace());
        assertEquals("1", dto.getOrgId());
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
    }

    @Test
    void testOrgTaxonomyToDtoWithDifferentNamespaces() {
        // Arrange
        String[] namespaces = {
                "company_types",
                "tags",
                "departments",
                "skill_levels",
                "project_statuses"
        };

        for (String namespace : namespaces) {
            OrgTaxonomy taxonomy = new OrgTaxonomy();
            taxonomy.setId(1L);
            taxonomy.setNamespace(namespace);
            taxonomy.setOrganization(organization);
            taxonomy.setCreatedAt(LocalDateTime.now());

            // Act
            OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

            // Assert
            assertEquals(namespace, dto.getNamespace());
        }
    }

    @Test
    void testOrgTaxonomyToDtoPreservesDates() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 5, 10, 15, 30);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 18, 14, 45, 00);

        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setId(3L);
        taxonomy.setNamespace("test_ns");
        taxonomy.setOrganization(organization);
        taxonomy.setCreatedAt(created);
        taxonomy.setUpdatedAt(updated);

        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

        // Assert
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
    }

    @Test
    void testOrgTaxonomyToDtoWithDifferentOrganizations() {
        // Arrange
        Organization org2 = new Organization();
        org2.setId(42L);

        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setId(4L);
        taxonomy.setNamespace("test");
        taxonomy.setOrganization(org2);
        taxonomy.setCreatedAt(LocalDateTime.now());

        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

        // Assert
        assertEquals("42", dto.getOrgId());
    }

    @Test
    void testOrgTaxonomyToDtoWithDifferentIds() {
        // Arrange
        long[] ids = {1L, 100L, 999999L, Long.MAX_VALUE / 2};

        for (long id : ids) {
            OrgTaxonomy taxonomy = new OrgTaxonomy();
            taxonomy.setId(id);
            taxonomy.setNamespace("test");
            taxonomy.setOrganization(organization);
            taxonomy.setCreatedAt(LocalDateTime.now());

            // Act
            OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

            // Assert
            assertEquals(id, dto.getId());
        }
    }

    @Test
    void testOrgTaxonomyToDtoWithNullNamespace() {
        // Arrange
        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setId(5L);
        taxonomy.setNamespace(null);
        taxonomy.setOrganization(organization);
        taxonomy.setCreatedAt(LocalDateTime.now());

        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

        // Assert
        assertNull(dto.getNamespace());
    }

    @Test
    void testOrgTaxonomyToDtoWithEmptyNamespace() {
        // Arrange
        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setId(6L);
        taxonomy.setNamespace("");
        taxonomy.setOrganization(organization);
        taxonomy.setCreatedAt(LocalDateTime.now());

        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

        // Assert
        assertEquals("", dto.getNamespace());
    }

    @Test
    void testOrgTaxonomyToDtoWithSpecialCharactersInNamespace() {
        // Arrange
        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setId(7L);
        taxonomy.setNamespace("namespace-with_special.chars:123");
        taxonomy.setOrganization(organization);
        taxonomy.setCreatedAt(LocalDateTime.now());

        // Act
        OrgTaxonomyResponseDto dto = mapper.toDto(taxonomy);

        // Assert
        assertEquals("namespace-with_special.chars:123", dto.getNamespace());
    }
}



