package com.mytegroup.api.mapper.estimates;

import com.mytegroup.api.dto.estimates.CreateEstimateDto;
import com.mytegroup.api.dto.estimates.EstimateLineItemDto;
import com.mytegroup.api.dto.estimates.UpdateEstimateDto;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstimateMapperUnitTest {

    private EstimateMapper estimateMapper;
    private Project project;
    private Organization organization;
    private User user;

    @BeforeEach
    void setUp() {
        estimateMapper = new EstimateMapper();
        project = new Project();
        project.setId(1L);
        organization = new Organization();
        organization.setId(1L);
        user = new User();
        user.setId("user1");
    }

    @Test
    void testCreateEstimateDtoToEntity() {
        // Arrange
        EstimateLineItemDto lineItem = new EstimateLineItemDto(
            "ITEM001",
            "Steel Beam",
            10.0,
            "pcs",
            1000.0,
            10000.0
        );

        CreateEstimateDto dto = new CreateEstimateDto(
            "Estimate 1",
            "Project estimate",
            Arrays.asList(lineItem),
            "Initial estimate notes"
        );

        // Act
        Estimate estimate = estimateMapper.toEntity(dto, project, organization, user);

        // Assert
        assertNotNull(estimate);
        assertEquals("Estimate 1", estimate.getName());
        assertEquals("Project estimate", estimate.getDescription());
        assertEquals("Initial estimate notes", estimate.getNotes());
        assertEquals(project, estimate.getProject());
        assertEquals(organization, estimate.getOrganization());
        assertEquals(user, estimate.getCreatedByUser());
        assertNotNull(estimate.getLineItems());
        assertEquals(1, estimate.getLineItems().size());
    }

    @Test
    void testUpdateEstimateDtoToEntity() {
        // Arrange
        Estimate estimate = new Estimate();
        estimate.setName("Old Name");
        estimate.setDescription("Old Description");

        UpdateEstimateDto dto = new UpdateEstimateDto(
            "Updated Name",
            "Updated Description",
            null,
            "Updated notes"
        );

        // Act
        estimateMapper.updateEntity(estimate, dto);

        // Assert
        assertEquals("Updated Name", estimate.getName());
        assertEquals("Updated Description", estimate.getDescription());
        assertEquals("Updated notes", estimate.getNotes());
    }

    @Test
    void testCreateEstimateWithoutLineItems() {
        // Arrange
        CreateEstimateDto dto = new CreateEstimateDto(
            "Empty Estimate",
            null,
            null,
            null
        );

        // Act
        Estimate estimate = estimateMapper.toEntity(dto, project, organization, user);

        // Assert
        assertNotNull(estimate);
        assertEquals("Empty Estimate", estimate.getName());
        assertNotNull(estimate.getLineItems());
        assertTrue(estimate.getLineItems().isEmpty());
    }
}

