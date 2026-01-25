package com.mytegroup.api.mapper.graphedges;

import com.mytegroup.api.dto.graphedges.CreateGraphEdgeDto;
import com.mytegroup.api.entity.organization.GraphEdge;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphEdgeMapperUnitTest {

    private GraphEdgeMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new GraphEdgeMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Organization");
    }

    @Test
    void testCreateGraphEdgeDtoToEntity() {
        // Arrange
        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("COMPANY");
        dto.setFromId("100");
        dto.setToType("COMPANY_LOCATION");
        dto.setToId("200");
        dto.setEdgeType("located_in");
        dto.setMeta(new HashMap<>());
        dto.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        dto.setEffectiveTo(LocalDate.of(2024, 12, 31));

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(edge);
        assertEquals(organization, edge.getOrganization());
        assertEquals(GraphNodeType.COMPANY, edge.getFromNodeType());
        assertEquals(100L, edge.getFromNodeId());
        assertEquals(GraphNodeType.COMPANY_LOCATION, edge.getToNodeType());
        assertEquals(200L, edge.getToNodeId());
        assertEquals("located_in", edge.getEdgeTypeKey());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0, 0), edge.getEffectiveFrom());
        assertEquals(LocalDateTime.of(2024, 12, 31, 23, 59, 59), edge.getEffectiveTo());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityWithNullDates() {
        // Arrange
        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("PERSON");
        dto.setFromId("1");
        dto.setToType("COMPANY");
        dto.setToId("2");
        dto.setEdgeType("works_for");
        dto.setEffectiveFrom(null);
        dto.setEffectiveTo(null);

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertNull(edge.getEffectiveFrom());
        assertNull(edge.getEffectiveTo());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityWithNullNodeTypes() {
        // Arrange
        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType(null);
        dto.setFromId("1");
        dto.setToType(null);
        dto.setToId("2");
        dto.setEdgeType("test");

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertNull(edge.getFromNodeType());
        assertNull(edge.getToNodeType());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityWithNullIds() {
        // Arrange
        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("COMPANY");
        dto.setFromId(null);
        dto.setToType("COMPANY_LOCATION");
        dto.setToId(null);
        dto.setEdgeType("test");

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertNull(edge.getFromNodeId());
        assertNull(edge.getToNodeId());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityWithNullMetadata() {
        // Arrange
        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("COMPANY");
        dto.setFromId("1");
        dto.setToType("COMPANY_LOCATION");
        dto.setToId("2");
        dto.setEdgeType("test");
        dto.setMeta(null);

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertNull(edge.getMetadata());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityWithMetadata() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("relationship_type", "primary");
        metadata.put("confidence", 0.95);

        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("COMPANY");
        dto.setFromId("1");
        dto.setToType("COMPANY_LOCATION");
        dto.setToId("2");
        dto.setEdgeType("test");
        dto.setMeta(metadata);

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(metadata, edge.getMetadata());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityWithAllNodeTypes() {
        // Arrange
        GraphNodeType[] nodeTypes = {
                GraphNodeType.COMPANY,
                GraphNodeType.PERSON,
                GraphNodeType.ORG_LOCATION,
                GraphNodeType.COMPANY_LOCATION
        };

        for (GraphNodeType nodeType : nodeTypes) {
            CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
            dto.setFromType(nodeType.getValue().toUpperCase());
            dto.setFromId("1");
            dto.setToType(nodeType.getValue().toUpperCase());
            dto.setToId("2");
            dto.setEdgeType("test");

            // Act
            GraphEdge edge = mapper.toEntity(dto, organization);

            // Assert
            assertEquals(nodeType, edge.getFromNodeType());
            assertEquals(nodeType, edge.getToNodeType());
        }
    }

    @Test
    void testCreateGraphEdgeDtoToEntityMapsAllFields() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("COMPANY");
        dto.setFromId("555");
        dto.setToType("PERSON");
        dto.setToId("666");
        dto.setEdgeType("employs");
        dto.setMeta(metadata);
        dto.setEffectiveFrom(LocalDate.of(2024, 6, 1));
        dto.setEffectiveTo(LocalDate.of(2024, 12, 31));

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(organization, edge.getOrganization());
        assertEquals(GraphNodeType.COMPANY, edge.getFromNodeType());
        assertEquals(555L, edge.getFromNodeId());
        assertEquals(GraphNodeType.PERSON, edge.getToNodeType());
        assertEquals(666L, edge.getToNodeId());
        assertEquals("employs", edge.getEdgeTypeKey());
        assertEquals(metadata, edge.getMetadata());
        assertEquals(LocalDateTime.of(2024, 6, 1, 0, 0, 0), edge.getEffectiveFrom());
        assertEquals(LocalDateTime.of(2024, 12, 31, 23, 59, 59), edge.getEffectiveTo());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityDateConversion() {
        // Arrange
        LocalDate fromDate = LocalDate.of(2024, 3, 15);
        LocalDate toDate = LocalDate.of(2024, 9, 30);

        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("COMPANY");
        dto.setFromId("1");
        dto.setToType("COMPANY_LOCATION");
        dto.setToId("2");
        dto.setEdgeType("test");
        dto.setEffectiveFrom(fromDate);
        dto.setEffectiveTo(toDate);

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(LocalDateTime.of(2024, 3, 15, 0, 0, 0), edge.getEffectiveFrom());
        assertEquals(LocalDateTime.of(2024, 9, 30, 23, 59, 59), edge.getEffectiveTo());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityWithDifferentIds() {
        // Arrange
        String[] ids = {"1", "100", "9999", "12345"};

        for (String id : ids) {
            CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
            dto.setFromType("COMPANY");
            dto.setFromId(id);
            dto.setToType("COMPANY_LOCATION");
            dto.setToId(id);
            dto.setEdgeType("test");

            // Act
            GraphEdge edge = mapper.toEntity(dto, organization);

            // Assert
            assertEquals(Long.parseLong(id), edge.getFromNodeId());
            assertEquals(Long.parseLong(id), edge.getToNodeId());
        }
    }

    @Test
    void testCreateGraphEdgeDtoToEntityCaseSensitivity() {
        // Arrange
        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("company"); // lowercase
        dto.setFromId("1");
        dto.setToType("person"); // lowercase
        dto.setToId("2");
        dto.setEdgeType("test");

        // Act
        GraphEdge edge = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(GraphNodeType.COMPANY, edge.getFromNodeType());
        assertEquals(GraphNodeType.PERSON, edge.getToNodeType());
    }

    @Test
    void testCreateGraphEdgeDtoToEntityPreservesEdgeType() {
        // Arrange
        String[] edgeTypes = {
                "works_for",
                "located_in",
                "manages",
                "reports_to",
                "parent_company"
        };

        for (String edgeType : edgeTypes) {
            CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
            dto.setFromType("COMPANY");
            dto.setFromId("1");
            dto.setToType("COMPANY_LOCATION");
            dto.setToId("2");
            dto.setEdgeType(edgeType);

            // Act
            GraphEdge edge = mapper.toEntity(dto, organization);

            // Assert
            assertEquals(edgeType, edge.getEdgeTypeKey());
        }
    }
}

