package com.mytegroup.api.mapper.projects;

import com.mytegroup.api.dto.projects.CreateProjectDto;
import com.mytegroup.api.dto.projects.UpdateProjectDto;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectMapperUnitTest {

    private ProjectMapper projectMapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        projectMapper = new ProjectMapper();
        organization = new Organization();
        organization.setId(1L);
    }

    @Test
    void testCreateProjectDtoToEntity() {
        // Arrange
        LocalDate bidDate = LocalDate.of(2024, 1, 15);
        Map<String, Object> budget = new HashMap<>();
        budget.put("amount", 50000.0);
        budget.put("currency", "USD");
        budget.put("hours", 500.0);
        budget.put("labourRate", 100.0);

        CreateProjectDto dto = new CreateProjectDto(
            "Test Project",
            "Description",
            "PRJ001",
            "Active",
            "Main Location",
            bidDate,
            null,
            null,
            null,
            null,
            null,
            budget
        );

        // Act
        Project project = projectMapper.toEntity(dto, organization, null);

        // Assert
        assertNotNull(project);
        assertEquals("Test Project", project.getName());
        assertEquals("Description", project.getDescription());
        assertEquals("PRJ001", project.getProjectCode());
        assertEquals("Active", project.getStatus());
        assertEquals("Main Location", project.getLocation());
        assertEquals(bidDate, project.getBidDate());
        assertEquals(organization, project.getOrganization());
        assertNotNull(project.getBudget());
        assertEquals(50000.0, project.getBudget().getAmount());
    }

    @Test
    void testUpdateProjectDtoToEntity() {
        // Arrange
        Project project = new Project();
        project.setName("Old Name");
        project.setDescription("Old Description");

        UpdateProjectDto dto = new UpdateProjectDto(
            "New Name",
            "New Description",
            "PRJ002",
            "Completed",
            "New Location",
            null,
            null,
            null,
            null,
            null,
            null
        );

        // Act
        projectMapper.updateEntity(project, dto, null);

        // Assert
        assertEquals("New Name", project.getName());
        assertEquals("New Description", project.getDescription());
        assertEquals("PRJ002", project.getProjectCode());
        assertEquals("Completed", project.getStatus());
        assertEquals("New Location", project.getLocation());
    }

    @Test
    void testProjectDtoWithoutBudget() {
        // Arrange
        CreateProjectDto dto = new CreateProjectDto(
            "Project Name",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        // Act
        Project project = projectMapper.toEntity(dto, organization, null);

        // Assert
        assertNotNull(project);
        assertEquals("Project Name", project.getName());
        assertNull(project.getBudget());
    }
}

