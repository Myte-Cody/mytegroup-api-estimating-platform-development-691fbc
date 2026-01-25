package com.mytegroup.api.mapper.estimates;

import com.mytegroup.api.dto.response.EstimateResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.projects.EstimateStatus;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EstimateMapperToDtoUnitTest {

    private EstimateMapper mapper;
    private Organization organization;
    private Project project;

    @BeforeEach
    void setUp() {
        mapper = new EstimateMapper();
        organization = new Organization();
        organization.setId(1L);

        project = new Project();
        project.setId(1L);
    }

    @Test
    void testToDtoWithFullEntity() {
        // Arrange
        Estimate entity = new Estimate();
        entity.setId(10L);
        entity.setProject(project);
        entity.setName("Test Estimate");
        entity.setDescription("Test description");
        entity.setStatus(EstimateStatus.FINAL);
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        EstimateResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals(1L, dto.getProjectId());
        assertEquals("Test Estimate", dto.getEstimateName());
        assertEquals("Test description", dto.getDescription());
        assertEquals("final", dto.getStatus());
        assertEquals("1", dto.getOrgId());
    }

    @Test
    void testToDtoNullEntity() {
        // Act
        EstimateResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDtoWithoutProject() {
        // Arrange
        Estimate entity = new Estimate();
        entity.setId(11L);
        entity.setProject(null);
        entity.setName("No Project");
        entity.setOrganization(organization);

        // Act
        EstimateResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getProjectId());
    }

    @Test
    void testToDoDifferentStatuses() {
        // Arrange
        EstimateStatus[] statuses = {
                EstimateStatus.DRAFT,
                EstimateStatus.FINAL,
                EstimateStatus.ARCHIVED
        };

        for (EstimateStatus status : statuses) {
            Estimate entity = new Estimate();
            entity.setId(12L);
            entity.setProject(project);
            entity.setName("Test");
            entity.setStatus(status);
            entity.setOrganization(organization);

            // Act
            EstimateResponseDto dto = mapper.toDto(entity);

            // Assert
            assertEquals(status.getValue(), dto.getStatus());
        }
    }

    @Test
    void testToDtoMapsAllFields() {
        // Arrange
        Estimate entity = new Estimate();
        entity.setId(13L);
        entity.setProject(project);
        entity.setName("Complete Estimate");
        entity.setDescription("Complete description");
        entity.setStatus(EstimateStatus.FINAL);
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 30, 0));

        // Act
        EstimateResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Complete Estimate", dto.getEstimateName());
        assertEquals("Complete description", dto.getDescription());
        assertEquals("final", dto.getStatus());
        assertEquals("1", dto.getOrgId());
    }
}

