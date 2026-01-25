package com.mytegroup.api.mapper.projects;

import com.mytegroup.api.dto.response.ProjectResponseDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.projects.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProjectMapperUnitTest {

    private ProjectMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new ProjectMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testToDtoWithFullEntity() {
        // Arrange
        Project entity = new Project();
        entity.setId(10L);
        entity.setName("Test Project");
        entity.setDescription("Test description");
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        ProjectResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Test Project", dto.getName());
        assertEquals("Test description", dto.getDescription());
        assertEquals("1", dto.getOrgId());
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0, 0), dto.getCreatedAt());
    }

    @Test
    void testToDtoNullEntity() {
        // Act
        ProjectResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDtoWithoutOrganization() {
        // Arrange
        Project entity = new Project();
        entity.setId(11L);
        entity.setName("No Org Project");
        entity.setDescription("Description");
        entity.setOrganization(null);

        // Act
        ProjectResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getOrgId());
        assertEquals("No Org Project", dto.getName());
    }

    @Test
    void testToDtoWithMinimalFields() {
        // Arrange
        Project entity = new Project();
        entity.setId(12L);
        entity.setName("Minimal");
        entity.setOrganization(organization);

        // Act
        ProjectResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Minimal", dto.getName());
        assertNull(dto.getDescription());
        assertEquals("1", dto.getOrgId());
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        Project entity = new Project();
        entity.setId(13L);
        entity.setName("Complete Project");
        entity.setDescription("Complete description");
        entity.setOrganization(organization);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 30, 0));

        // Act
        ProjectResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Complete Project", dto.getName());
        assertEquals("Complete description", dto.getDescription());
        assertEquals("1", dto.getOrgId());
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 15, 11, 30, 0), dto.getUpdatedAt());
    }

    @Test
    void testToDoDifferentProjects() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            Project entity = new Project();
            entity.setId((long) (20 + i));
            entity.setName("Project " + i);
            entity.setDescription("Description " + i);
            entity.setOrganization(organization);

            // Act
            ProjectResponseDto dto = mapper.toDto(entity);

            // Assert
            assertEquals("Project " + i, dto.getName());
            assertEquals("Description " + i, dto.getDescription());
            assertEquals("1", dto.getOrgId());
        }
    }

    @Test
    void testToDoBuildWithNullDescription() {
        // Arrange
        Project entity = new Project();
        entity.setId(14L);
        entity.setName("No Description");
        entity.setDescription(null);
        entity.setOrganization(organization);

        // Act
        ProjectResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("No Description", dto.getName());
        assertNull(dto.getDescription());
    }

    @Test
    void testToDoBuildWithNullStatus() {
        // Arrange
        Project entity = new Project();
        entity.setId(15L);
        entity.setName("No Status");
        entity.setStatus(null);
        entity.setOrganization(organization);

        // Act
        ProjectResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getStatus());
        assertEquals("No Status", dto.getName());
    }

    @Test
    void testToDoBuildWithNullDates() {
        // Arrange
        Project entity = new Project();
        entity.setId(16L);
        entity.setName("No Dates");
        entity.setOrganization(organization);
        entity.setCreatedAt(null);
        entity.setUpdatedAt(null);

        // Act
        ProjectResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getCreatedAt());
        assertNull(dto.getUpdatedAt());
    }
}

