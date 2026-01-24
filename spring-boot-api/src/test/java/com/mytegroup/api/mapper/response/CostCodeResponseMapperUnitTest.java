package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.CostCodeResponseDto;
import com.mytegroup.api.entity.costcodes.CostCode;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CostCodeResponseMapperUnitTest {

    private CostCodeResponseMapper costCodeResponseMapper;

    @BeforeEach
    void setUp() {
        costCodeResponseMapper = new CostCodeResponseMapper();
    }

    @Test
    void testCostCodeEntityToResponseDto() {
        // Arrange
        Organization org = new Organization();
        org.setId(1L);

        CostCode costCode = new CostCode();
        costCode.setId(5L);
        costCode.setCode("CC001");
        costCode.setName("Labor");
        costCode.setDescription("Direct labor costs");
        costCode.setActive(true);
        costCode.setOrganization(org);
        costCode.setCreatedAt(LocalDateTime.now());
        costCode.setUpdatedAt(LocalDateTime.now());

        // Act
        CostCodeResponseDto dto = costCodeResponseMapper.toDto(costCode);

        // Assert
        assertNotNull(dto);
        assertEquals(5L, dto.id());
        assertEquals("CC001", dto.code());
        assertEquals("Labor", dto.name());
        assertEquals("Direct labor costs", dto.description());
        assertTrue(dto.active());
        assertEquals(1L, dto.orgId());
    }

    @Test
    void testCostCodeInactive() {
        // Arrange
        Organization org = new Organization();
        org.setId(2L);

        CostCode costCode = new CostCode();
        costCode.setId(6L);
        costCode.setCode("CC002");
        costCode.setName("Materials");
        costCode.setDescription(null);
        costCode.setActive(false);
        costCode.setOrganization(org);

        // Act
        CostCodeResponseDto dto = costCodeResponseMapper.toDto(costCode);

        // Assert
        assertNotNull(dto);
        assertEquals(6L, dto.id());
        assertEquals("CC002", dto.code());
        assertEquals("Materials", dto.name());
        assertNull(dto.description());
        assertFalse(dto.active());
    }

    @Test
    void testNullEntityReturnsNull() {
        // Act
        CostCodeResponseDto dto = costCodeResponseMapper.toDto(null);

        // Assert
        assertNull(dto);
    }
}

