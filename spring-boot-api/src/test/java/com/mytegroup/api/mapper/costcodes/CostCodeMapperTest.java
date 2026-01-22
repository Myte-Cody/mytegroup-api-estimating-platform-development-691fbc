package com.mytegroup.api.mapper.costcodes;

import com.mytegroup.api.dto.costcodes.CreateCostCodeDto;
import com.mytegroup.api.dto.costcodes.CostCodeInputDto;
import com.mytegroup.api.dto.costcodes.UpdateCostCodeDto;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CostCodeMapper.
 */
class CostCodeMapperTest {

    private CostCodeMapper mapper;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        mapper = new CostCodeMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreateCostCodeDto dto = new CreateCostCodeDto(
                "Labor",
                "LAB-001",
                "Labor cost code"
        );

        // When
        CostCode costCode = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(costCode).isNotNull();
        assertThat(costCode.getCategory()).isEqualTo("Labor");
        assertThat(costCode.getCode()).isEqualTo("LAB-001");
        assertThat(costCode.getDescription()).isEqualTo("Labor cost code");
        assertThat(costCode.getOrganization()).isEqualTo(testOrg);
        assertThat(costCode.getActive()).isTrue();
    }

    @Test
    void shouldMapCostCodeInputDtoToEntity() {
        // Given
        CostCodeInputDto dto = new CostCodeInputDto(
                "Material",
                "MAT-001",
                "Material cost code"
        );

        // When
        CostCode costCode = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(costCode).isNotNull();
        assertThat(costCode.getCategory()).isEqualTo("Material");
        assertThat(costCode.getCode()).isEqualTo("MAT-001");
        assertThat(costCode.getActive()).isTrue();
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        CostCode costCode = new CostCode();
        costCode.setCategory("Original");
        costCode.setCode("ORIG-001");

        UpdateCostCodeDto dto = new UpdateCostCodeDto(
                "Updated",
                "UPD-001",
                "Updated description"
        );

        // When
        mapper.updateEntity(costCode, dto);

        // Then
        assertThat(costCode.getCategory()).isEqualTo("Updated");
        assertThat(costCode.getCode()).isEqualTo("UPD-001");
        assertThat(costCode.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        CostCode costCode = new CostCode();
        costCode.setCategory("Original");
        costCode.setCode("ORIG-001");

        UpdateCostCodeDto dto = new UpdateCostCodeDto(
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(costCode, dto);

        // Then
        assertThat(costCode.getCategory()).isEqualTo("Original");
        assertThat(costCode.getCode()).isEqualTo("ORIG-001");
    }
}

