package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.EstimateResponseDto;
import com.mytegroup.api.entity.estimates.Estimate;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.projects.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EstimateResponseMapperUnitTest {

    private EstimateResponseMapper estimateResponseMapper;

    @BeforeEach
    void setUp() {
        estimateResponseMapper = new EstimateResponseMapper();
    }

    @Test
    void testEstimateEntityToResponseDto() {
        // Arrange
        Organization org = new Organization();
        org.setId(1L);

        Project project = new Project();
        project.setId(10L);

        Estimate estimate = new Estimate();
        estimate.setId(100L);
        estimate.setEstimateName("Steel Estimate");
        estimate.setDescription("Estimate for steel components");
        estimate.setStatus("DRAFT");
        estimate.setProject(project);
        estimate.setOrganization(org);
        estimate.setCreatedAt(LocalDateTime.now());
        estimate.setUpdatedAt(LocalDateTime.now());

        // Act
        EstimateResponseDto dto = estimateResponseMapper.toDto(estimate);

        // Assert
        assertNotNull(dto);
        assertEquals(100L, dto.id());
        assertEquals("Steel Estimate", dto.estimateName());
        assertEquals("Estimate for steel components", dto.description());
        assertEquals("DRAFT", dto.status());
        assertEquals(1L, dto.orgId());
        assertEquals(10L, dto.projectId());
    }

    @Test
    void testEstimateWithNullProject() {
        // Arrange
        Organization org = new Organization();
        org.setId(2L);

        Estimate estimate = new Estimate();
        estimate.setId(200L);
        estimate.setEstimateName("Empty Estimate");
        estimate.setProject(null);
        estimate.setOrganization(org);

        // Act
        EstimateResponseDto dto = estimateResponseMapper.toDto(estimate);

        // Assert
        assertNotNull(dto);
        assertEquals(200L, dto.id());
        assertEquals("Empty Estimate", dto.estimateName());
        assertNull(dto.projectId());
    }

    @Test
    void testNullEntityReturnsNull() {
        // Act
        EstimateResponseDto dto = estimateResponseMapper.toDto(null);

        // Assert
        assertNull(dto);
    }
}

