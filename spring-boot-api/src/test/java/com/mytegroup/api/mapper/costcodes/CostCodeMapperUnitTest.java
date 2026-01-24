package com.mytegroup.api.mapper.costcodes;

import com.mytegroup.api.dto.costcodes.CreateCostCodeDto;
import com.mytegroup.api.dto.costcodes.UpdateCostCodeDto;
import com.mytegroup.api.entity.costcodes.CostCode;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class CostCodeMapperUnitTest {

    private CostCodeMapper costCodeMapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        costCodeMapper = new CostCodeMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testCreateCostCodeDtoToEntity() {
        // Arrange
        CreateCostCodeDto dto = new CreateCostCodeDto(
            "CC001",
            "Labor",
            "Direct labor costs"
        );

        // Act
        CostCode costCode = costCodeMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(costCode);
        assertEquals("CC001", costCode.getCode());
        assertEquals("Labor", costCode.getName());
        assertEquals("Direct labor costs", costCode.getDescription());
        assertEquals(organization, costCode.getOrganization());
    }

    @Test
    void testUpdateCostCodeDtoToEntity() {
        // Arrange
        CostCode costCode = new CostCode();
        costCode.setCode("CC001");
        costCode.setName("Old Name");

        UpdateCostCodeDto dto = new UpdateCostCodeDto(
            "New Name",
            "New description"
        );

        // Act
        costCodeMapper.updateEntity(costCode, dto);

        // Assert
        assertEquals("CC001", costCode.getCode());
        assertEquals("New Name", costCode.getName());
        assertEquals("New description", costCode.getDescription());
    }

    @Test
    void testCreateCostCodeWithMinimalData() {
        // Arrange
        CreateCostCodeDto dto = new CreateCostCodeDto(
            "CC002",
            "Materials",
            null
        );

        // Act
        CostCode costCode = costCodeMapper.toEntity(dto, organization);

        // Assert
        assertNotNull(costCode);
        assertEquals("CC002", costCode.getCode());
        assertEquals("Materials", costCode.getName());
        assertNull(costCode.getDescription());
    }
}

