package com.mytegroup.api.mapper.costcodes;

import com.mytegroup.api.dto.costcodes.CreateCostCodeDto;
import com.mytegroup.api.dto.costcodes.CostCodeInputDto;
import com.mytegroup.api.dto.costcodes.UpdateCostCodeDto;
import com.mytegroup.api.dto.response.CostCodeResponseDto;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CostCodeMapperUnitTest {

    private CostCodeMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new CostCodeMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Organization");
    }

    // ===== toEntity(CreateCostCodeDto, Organization) Tests =====

    @Test
    void testCreateCostCodeDtoToEntity() {
        // Arrange
        CreateCostCodeDto dto = new CreateCostCodeDto("LABOR", "CC001", "Labor costs");

        // Act
        CostCode costCode = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(costCode);
        assertEquals(organization, costCode.getOrganization());
        assertEquals("LABOR", costCode.getCategory());
        assertEquals("CC001", costCode.getCode());
        assertEquals("Labor costs", costCode.getDescription());
        assertTrue(costCode.getActive());
    }

    @Test
    void testCreateCostCodeDtoToEntityWithNullDescription() {
        // Arrange
        CreateCostCodeDto dto = new CreateCostCodeDto("MATERIALS", "CC002", null);

        // Act
        CostCode costCode = mapper.toEntity(dto, organization);

        // Assert
        assertNull(costCode.getDescription());
        assertEquals("MATERIALS", costCode.getCategory());
        assertTrue(costCode.getActive());
    }

    @Test
    void testCreateCostCodeDtoToEntitySetsActiveTrue() {
        // Arrange
        CreateCostCodeDto dto = new CreateCostCodeDto("EQUIPMENT", "CC003", "Equipment rental");

        // Act
        CostCode costCode = mapper.toEntity(dto, organization);

        // Assert
        assertTrue(costCode.getActive());
    }

    @Test
    void testCreateCostCodeDtoToEntityMapsAllFields() {
        // Arrange
        CreateCostCodeDto dto = new CreateCostCodeDto("OVERHEAD", "CC004", "Overhead expenses");

        // Act
        CostCode costCode = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(organization, costCode.getOrganization());
        assertEquals("OVERHEAD", costCode.getCategory());
        assertEquals("CC004", costCode.getCode());
        assertEquals("Overhead expenses", costCode.getDescription());
        assertTrue(costCode.getActive());
    }

    @Test
    void testCreateCostCodeDtoToEntityWithEmptyStrings() {
        // Arrange
        CreateCostCodeDto dto = new CreateCostCodeDto("", "", "");

        // Act
        CostCode costCode = mapper.toEntity(dto, organization);

        // Assert
        assertEquals("", costCode.getCategory());
        assertEquals("", costCode.getCode());
        assertEquals("", costCode.getDescription());
    }

    // ===== toEntity(CostCodeInputDto, Organization) Tests =====

    @Test
    void testCostCodeInputDtoToEntity() {
        // Arrange
        CostCodeInputDto dto = new CostCodeInputDto("LABOR", "CC101", "Labor input");

        // Act
        CostCode costCode = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(costCode);
        assertEquals(organization, costCode.getOrganization());
        assertEquals("LABOR", costCode.getCategory());
        assertEquals("CC101", costCode.getCode());
        assertEquals("Labor input", costCode.getDescription());
        assertTrue(costCode.getActive());
    }

    @Test
    void testCostCodeInputDtoToEntityWithNullDescription() {
        // Arrange
        CostCodeInputDto dto = new CostCodeInputDto("MATERIALS", "CC102", null);

        // Act
        CostCode costCode = mapper.toEntity(dto, organization);

        // Assert
        assertNull(costCode.getDescription());
        assertTrue(costCode.getActive());
    }

    @Test
    void testCostCodeInputDtoToEntityMapsAllFields() {
        // Arrange
        CostCodeInputDto dto = new CostCodeInputDto("TOOLS", "CC103", "Tools and equipment");

        // Act
        CostCode costCode = mapper.toEntity(dto, organization);

        // Assert
        assertEquals("TOOLS", costCode.getCategory());
        assertEquals("CC103", costCode.getCode());
        assertEquals("Tools and equipment", costCode.getDescription());
    }

    // ===== updateEntity Tests =====

    @Test
    void testUpdateEntity() {
        // Arrange
        CostCode costCode = new CostCode();
        costCode.setCategory("OLD_CATEGORY");
        costCode.setCode("OLD_CODE");
        costCode.setDescription("Old description");

        UpdateCostCodeDto dto = new UpdateCostCodeDto("NEW_CATEGORY", "NEW_CODE", "New description");

        // Act
        mapper.updateEntity(costCode, dto);

        // Assert
        assertEquals("NEW_CATEGORY", costCode.getCategory());
        assertEquals("NEW_CODE", costCode.getCode());
        assertEquals("New description", costCode.getDescription());
    }

    @Test
    void testUpdateEntityPartialUpdate() {
        // Arrange
        CostCode costCode = new CostCode();
        costCode.setCategory("ORIGINAL_CATEGORY");
        costCode.setCode("ORIGINAL_CODE");
        costCode.setDescription("Original description");

        UpdateCostCodeDto dto = new UpdateCostCodeDto(null, "UPDATED_CODE", null);

        // Act
        mapper.updateEntity(costCode, dto);

        // Assert
        assertEquals("ORIGINAL_CATEGORY", costCode.getCategory());
        assertEquals("UPDATED_CODE", costCode.getCode());
        assertEquals("Original description", costCode.getDescription());
    }

    @Test
    void testUpdateEntityWithAllNulls() {
        // Arrange
        CostCode costCode = new CostCode();
        costCode.setCategory("CATEGORY");
        costCode.setCode("CODE");
        costCode.setDescription("Description");

        UpdateCostCodeDto dto = new UpdateCostCodeDto(null, null, null);

        // Act
        mapper.updateEntity(costCode, dto);

        // Assert
        assertEquals("CATEGORY", costCode.getCategory());
        assertEquals("CODE", costCode.getCode());
        assertEquals("Description", costCode.getDescription());
    }

    @Test
    void testUpdateEntityWithEmptyStrings() {
        // Arrange
        CostCode costCode = new CostCode();
        costCode.setCategory("CATEGORY");
        costCode.setCode("CODE");
        costCode.setDescription("Description");

        UpdateCostCodeDto dto = new UpdateCostCodeDto("", "", "");

        // Act
        mapper.updateEntity(costCode, dto);

        // Assert
        assertEquals("", costCode.getCategory());
        assertEquals("", costCode.getCode());
        assertEquals("", costCode.getDescription());
    }

    // ===== toDto Tests =====

    @Test
    void testToDtoFullEntity() {
        // Arrange
        CostCode entity = new CostCode();
        entity.setId(10L);
        entity.setCategory("LABOR");
        entity.setCode("CC001");
        entity.setDescription("Labor costs");
        entity.setActive(true);
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        CostCodeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("CC001", dto.getCode());
        assertEquals("LABOR", dto.getName());
        assertEquals("Labor costs", dto.getDescription());
        assertTrue(dto.getActive());
        assertEquals("1", dto.getOrgId());
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 1, 11, 0, 0), dto.getUpdatedAt());
    }

    @Test
    void testToDtoNullEntity() {
        // Act
        CostCodeResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDtoWithNullOrganization() {
        // Arrange
        CostCode entity = new CostCode();
        entity.setId(11L);
        entity.setCategory("MATERIALS");
        entity.setCode("CC002");
        entity.setDescription("Materials");
        entity.setActive(false);
        entity.setOrganization(null);

        // Act
        CostCodeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getOrgId());
        assertFalse(dto.getActive());
    }

    @Test
    void testToDtoMapsAllFields() {
        // Arrange
        CostCode entity = new CostCode();
        entity.setId(12L);
        entity.setCategory("EQUIPMENT");
        entity.setCode("CC003");
        entity.setDescription("Equipment rental");
        entity.setActive(true);
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 6, 15, 12, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 13, 30, 0));

        // Act
        CostCodeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals(12L, dto.getId());
        assertEquals("CC003", dto.getCode());
        assertEquals("EQUIPMENT", dto.getName());
        assertEquals("Equipment rental", dto.getDescription());
        assertEquals("1", dto.getOrgId());
    }

    @Test
    void testToDtoWithNullFields() {
        // Arrange
        CostCode entity = new CostCode();
        entity.setId(13L);
        entity.setCategory(null);
        entity.setCode(null);
        entity.setDescription(null);
        entity.setActive(null);
        entity.setOrganization(organization);

        // Act
        CostCodeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getName());
        assertNull(dto.getCode());
        assertNull(dto.getDescription());
        assertNull(dto.getActive());
    }
}



