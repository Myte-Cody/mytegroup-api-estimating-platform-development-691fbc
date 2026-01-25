package com.mytegroup.api.mapper.orgtaxonomy;

import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyDto;
import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyValueDto;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
import com.mytegroup.api.entity.organization.embeddable.OrgTaxonomyValue;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrgTaxonomyMapperUnitTest {

    private OrgTaxonomyMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new OrgTaxonomyMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Organization");
    }

    @Test
    void testPutOrgTaxonomyDtoToEntity() {
        // Arrange
        PutOrgTaxonomyValueDto value1 = new PutOrgTaxonomyValueDto("key1", "Label 1", 1, "#FF0000", null);
        PutOrgTaxonomyValueDto value2 = new PutOrgTaxonomyValueDto("key2", "Label 2", 2, "#00FF00", null);
        
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(value1, value2));

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(taxonomy);
        assertEquals(organization, taxonomy.getOrganization());
        assertNotNull(taxonomy.getValues());
        assertEquals(2, taxonomy.getValues().size());
        
        OrgTaxonomyValue val1 = taxonomy.getValues().get(0);
        assertEquals("key1", val1.getKey());
        assertEquals("Label 1", val1.getLabel());
        assertEquals(1, val1.getSortOrder());
        assertEquals("#FF0000", val1.getColor());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityWithEmptyValues() {
        // Arrange
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(new ArrayList<>());

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(taxonomy);
        assertEquals(organization, taxonomy.getOrganization());
        assertTrue(taxonomy.getValues().isEmpty());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityWithMetadata() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "category");
        metadata.put("count", 5);

        PutOrgTaxonomyValueDto value = new PutOrgTaxonomyValueDto("key1", "Label 1", 1, "#000000", metadata);
        
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(value));

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(taxonomy.getValues());
        assertEquals(1, taxonomy.getValues().size());
        OrgTaxonomyValue val = taxonomy.getValues().get(0);
        assertNotNull(val.getMetadata());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityWithMultipleValues() {
        // Arrange
        List<PutOrgTaxonomyValueDto> values = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            values.add(new PutOrgTaxonomyValueDto(
                "key" + i, 
                "Label " + i, 
                i, 
                "#" + String.format("%06X", i), 
                null
            ));
        }

        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(values);

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(5, taxonomy.getValues().size());
        for (int i = 0; i < 5; i++) {
            assertEquals("key" + i, taxonomy.getValues().get(i).getKey());
            assertEquals(i, taxonomy.getValues().get(i).getSortOrder());
        }
    }

    @Test
    void testPutOrgTaxonomyDtoToEntitySetsOrganization() {
        // Arrange
        PutOrgTaxonomyValueDto value = new PutOrgTaxonomyValueDto("key", "Label", 1, "#000000", null);
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(value));

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(organization, taxonomy.getOrganization());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityMapsAllValueFields() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("custom", "data");

        PutOrgTaxonomyValueDto value = new PutOrgTaxonomyValueDto(
            "complete_key",
            "Complete Label",
            99,
            "#ABCDEF",
            metadata
        );

        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(value));

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        OrgTaxonomyValue val = taxonomy.getValues().get(0);
        assertEquals("complete_key", val.getKey());
        assertEquals("Complete Label", val.getLabel());
        assertEquals(99, val.getSortOrder());
        assertEquals("#ABCDEF", val.getColor());
        assertNotNull(val.getMetadata());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityWithNullMetadata() {
        // Arrange
        PutOrgTaxonomyValueDto value = new PutOrgTaxonomyValueDto("key", "Label", 1, "#000000", null);
        
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(value));

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        OrgTaxonomyValue val = taxonomy.getValues().get(0);
        assertNull(val.getMetadata());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityPreservesKeyValues() {
        // Arrange
        List<PutOrgTaxonomyValueDto> values = new ArrayList<>();
        values.add(new PutOrgTaxonomyValueDto("alpha", "Alpha Label", 1, "#000000", null));
        values.add(new PutOrgTaxonomyValueDto("beta", "Beta Label", 2, "#111111", null));
        values.add(new PutOrgTaxonomyValueDto("gamma", "Gamma Label", 3, "#222222", null));

        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(values);

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        assertEquals("alpha", taxonomy.getValues().get(0).getKey());
        assertEquals("beta", taxonomy.getValues().get(1).getKey());
        assertEquals("gamma", taxonomy.getValues().get(2).getKey());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityPreservesSortOrder() {
        // Arrange
        List<PutOrgTaxonomyValueDto> values = new ArrayList<>();
        values.add(new PutOrgTaxonomyValueDto("key1", "Label 1", 100, "#000000", null));
        values.add(new PutOrgTaxonomyValueDto("key2", "Label 2", 50, "#111111", null));
        values.add(new PutOrgTaxonomyValueDto("key3", "Label 3", 200, "#222222", null));

        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(values);

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(100, taxonomy.getValues().get(0).getSortOrder());
        assertEquals(50, taxonomy.getValues().get(1).getSortOrder());
        assertEquals(200, taxonomy.getValues().get(2).getSortOrder());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityWithDifferentDifferentOrganizations() {
        // Arrange
        Organization org2 = new Organization();
        org2.setId(42L);

        PutOrgTaxonomyValueDto value = new PutOrgTaxonomyValueDto("key", "Label", 1, "#000000", null);
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(value));

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, org2);

        // Assert
        assertEquals(org2, taxonomy.getOrganization());
        assertEquals(42L, org2.getId());
    }

    @Test
    void testPutOrgTaxonomyDtoToEntityWithLargeNumberOfValues() {
        // Arrange
        List<PutOrgTaxonomyValueDto> values = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            values.add(new PutOrgTaxonomyValueDto(
                "key" + i, 
                "Label " + i, 
                i, 
                "#000000", 
                null
            ));
        }

        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(values);

        // Act
        OrgTaxonomy taxonomy = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(100, taxonomy.getValues().size());
    }
}


