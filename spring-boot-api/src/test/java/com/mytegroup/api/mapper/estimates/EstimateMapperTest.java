package com.mytegroup.api.mapper.estimates;

import com.mytegroup.api.dto.estimates.CreateEstimateDto;
import com.mytegroup.api.dto.estimates.EstimateLineItemDto;
import com.mytegroup.api.dto.estimates.UpdateEstimateDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.projects.EstimateStatus;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EstimateMapper.
 */
class EstimateMapperTest {

    private EstimateMapper mapper;
    private Project testProject;
    private Organization testOrg;
    private User testUser;

    @BeforeEach
    void setUp() {
        mapper = new EstimateMapper();
        testProject = new Project();
        testProject.setId(1L);
        testOrg = new Organization();
        testOrg.setId(1L);
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        EstimateLineItemDto lineItem = new EstimateLineItemDto(
                "CODE-001",
                "Description",
                10.0,
                "hours",
                50.0,
                500.0
        );
        CreateEstimateDto dto = new CreateEstimateDto(
                "Test Estimate",
                "Description",
                "Notes",
                List.of(lineItem)
        );

        // When
        Estimate estimate = mapper.toEntity(dto, testProject, testOrg, testUser);

        // Then
        assertThat(estimate).isNotNull();
        assertThat(estimate.getName()).isEqualTo("Test Estimate");
        assertThat(estimate.getProject()).isEqualTo(testProject);
        assertThat(estimate.getOrganization()).isEqualTo(testOrg);
        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.DRAFT);
        assertThat(estimate.getLineItems()).hasSize(1);
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        Estimate estimate = new Estimate();
        estimate.setName("Original");
        estimate.setStatus(EstimateStatus.DRAFT);

        UpdateEstimateDto dto = new UpdateEstimateDto(
                "Updated",
                "Updated description",
                "Updated notes",
                EstimateStatus.APPROVED,
                null
        );

        // When
        mapper.updateEntity(estimate, dto);

        // Then
        assertThat(estimate.getName()).isEqualTo("Updated");
        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.APPROVED);
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        Estimate estimate = new Estimate();
        estimate.setName("Original");
        estimate.setStatus(EstimateStatus.DRAFT);

        UpdateEstimateDto dto = new UpdateEstimateDto(
                null,
                null,
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(estimate, dto);

        // Then
        assertThat(estimate.getName()).isEqualTo("Original");
        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.DRAFT);
    }
}

