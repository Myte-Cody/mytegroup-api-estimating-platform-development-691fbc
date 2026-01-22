package com.mytegroup.api.mapper.orgtaxonomy;

import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyDto;
import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyValueDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrgTaxonomyMapper.
 */
class OrgTaxonomyMapperTest {

    private OrgTaxonomyMapper mapper;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        mapper = new OrgTaxonomyMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
    }

    @Test
    void shouldMapPutDtoToEntity() {
        // Given
        PutOrgTaxonomyValueDto valueDto = new PutOrgTaxonomyValueDto(
                "key1",
                "Label 1",
                1,
                "#FF0000",
                Map.of("meta", "value")
        );
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(valueDto));

        // When
        OrgTaxonomy taxonomy = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(taxonomy).isNotNull();
        assertThat(taxonomy.getOrganization()).isEqualTo(testOrg);
        assertThat(taxonomy.getValues()).hasSize(1);
        assertThat(taxonomy.getValues().get(0).getKey()).isEqualTo("key1");
        assertThat(taxonomy.getValues().get(0).getLabel()).isEqualTo("Label 1");
    }
}

