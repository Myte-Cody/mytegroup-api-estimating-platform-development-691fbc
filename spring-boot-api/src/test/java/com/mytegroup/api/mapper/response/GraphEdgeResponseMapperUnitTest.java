package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.GraphEdgeResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import com.mytegroup.api.entity.organization.GraphEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphEdgeResponseMapperUnitTest {

    private GraphEdgeResponseMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new GraphEdgeResponseMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testToDotFullEntity() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        GraphEdge entity = new GraphEdge();
        entity.setId(10L);
        entity.setFromNodeType(GraphNodeType.PERSON);
        entity.setFromNodeId(100L);
        entity.setToNodeType(GraphNodeType.COMPANY);
        entity.setToNodeId(200L);
        entity.setEdgeTypeKey("WORKS_FOR");
        entity.setMetadata(metadata);
        entity.setEffectiveFrom(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        entity.setEffectiveTo(LocalDateTime.of(2024, 12, 31, 23, 59, 59));
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));

        // Act
        GraphEdgeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("person", dto.getFromNodeType());
        assertEquals(100L, dto.getFromNodeId());
        assertEquals("company", dto.getToNodeType());
        assertEquals(200L, dto.getToNodeId());
        assertEquals("WORKS_FOR", dto.getEdgeTypeKey());
        assertEquals("1", dto.getOrgId());
    }

    @Test
    void testToDotNullEntity() {
        // Act
        GraphEdgeResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDoBuildsMapsWithDifferentNodeTypes() {
        // Arrange
        GraphEdge entity = new GraphEdge();
        entity.setId(11L);
        entity.setFromNodeType(GraphNodeType.ORG_LOCATION);
        entity.setFromNodeId(50L);
        entity.setToNodeType(GraphNodeType.COMPANY_LOCATION);
        entity.setToNodeId(75L);
        entity.setOrganization(organization);

        // Act
        GraphEdgeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("org_location", dto.getFromNodeType());
        assertEquals("company_location", dto.getToNodeType());
    }

    @Test
    void testToDoBuildsMapsWithNullOrganization() {
        // Arrange
        GraphEdge entity = new GraphEdge();
        entity.setId(12L);
        entity.setFromNodeType(GraphNodeType.PERSON);
        entity.setFromNodeId(1L);
        entity.setToNodeType(GraphNodeType.COMPANY);
        entity.setToNodeId(2L);
        entity.setOrganization(null);

        // Act
        GraphEdgeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getOrgId());
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("relationship", "manager");
        metadata.put("startDate", "2024-01-01");

        GraphEdge entity = new GraphEdge();
        entity.setId(13L);
        entity.setFromNodeType(GraphNodeType.PERSON);
        entity.setFromNodeId(100L);
        entity.setToNodeType(GraphNodeType.ORG_LOCATION);
        entity.setToNodeId(200L);
        entity.setEdgeTypeKey("MANAGES");
        entity.setMetadata(metadata);
        entity.setEffectiveFrom(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        entity.setEffectiveTo(LocalDateTime.of(2025, 12, 31, 23, 59, 59));
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

        // Act
        GraphEdgeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("MANAGES", dto.getEdgeTypeKey());
        assertEquals(metadata, dto.getMetadata());
        assertNotNull(dto.getEffectiveFrom());
        assertNotNull(dto.getEffectiveTo());
    }

    @Test
    void testToDoBuildsMapsWithNullMetadata() {
        // Arrange
        GraphEdge entity = new GraphEdge();
        entity.setId(14L);
        entity.setFromNodeType(GraphNodeType.COMPANY);
        entity.setFromNodeId(1L);
        entity.setToNodeType(GraphNodeType.COMPANY);
        entity.setToNodeId(2L);
        entity.setMetadata(null);
        entity.setOrganization(organization);

        // Act
        GraphEdgeResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getMetadata());
    }
}



