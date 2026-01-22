package com.mytegroup.api.mapper.projects;

import com.mytegroup.api.dto.projects.CreateProjectDto;
import com.mytegroup.api.dto.projects.UpdateProjectDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProjectMapper.
 */
class ProjectMapperTest {

    private ProjectMapper mapper;
    private Organization testOrg;
    private Office testOffice;

    @BeforeEach
    void setUp() {
        mapper = new ProjectMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
        testOffice = new Office();
        testOffice.setId(1L);
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        Map<String, Object> budget = Map.of(
                "hours", 100.0,
                "labourRate", 50.0,
                "currency", "USD",
                "amount", 5000.0
        );
        CreateProjectDto dto = new CreateProjectDto(
                "Test Project",
                "Description",
                "1",
                "1",
                "PROJ-001",
                "DRAFT",
                "Location",
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                budget,
                null,
                null,
                null
        );

        // When
        Project project = mapper.toEntity(dto, testOrg, testOffice);

        // Then
        assertThat(project).isNotNull();
        assertThat(project.getName()).isEqualTo("Test Project");
        assertThat(project.getOrganization()).isEqualTo(testOrg);
        assertThat(project.getOffice()).isEqualTo(testOffice);
        assertThat(project.getBudget()).isNotNull();
        assertThat(project.getBudget().getHours()).isEqualTo(100.0);
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        Project project = new Project();
        project.setName("Original");
        project.setStatus("DRAFT");

        UpdateProjectDto dto = new UpdateProjectDto(
                "Updated",
                null,
                null,
                null,
                "ACTIVE",
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

        // When
        mapper.updateEntity(project, dto, null);

        // Then
        assertThat(project.getName()).isEqualTo("Updated");
        assertThat(project.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        Project project = new Project();
        project.setName("Original");
        project.setStatus("DRAFT");

        UpdateProjectDto dto = new UpdateProjectDto(
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
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(project, dto, null);

        // Then
        assertThat(project.getName()).isEqualTo("Original");
        assertThat(project.getStatus()).isEqualTo("DRAFT");
    }
}

