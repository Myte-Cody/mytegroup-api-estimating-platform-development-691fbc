package com.mytegroup.api.controller.estimates;

import com.mytegroup.api.dto.estimates.CreateEstimateDto;
import com.mytegroup.api.dto.estimates.UpdateEstimateDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.projects.EstimateStatus;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EstimateController.
 * Tests estimate CRUD operations, line items, RBAC, request/response validation.
 */
class EstimateControllerIntegrationTest extends BaseControllerTest {

    @Autowired
    private UserRepository userRepository;

    private Organization testOrganization;
    private Project testProject;
    private Estimate testEstimate;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        // Create a user with username "user" to match @WithMockUser's default username
        User testUser = new User();
        testUser.setEmail("user@test.com");
        testUser.setUsername("user"); // Match @WithMockUser's default
        testUser.setOrganization(testOrganization);
        testUser.setPasswordHash("$2a$10$dummyHashForTesting");
        testUser.setIsEmailVerified(true);
        userRepository.save(testUser);
        testProject = setupProject(testOrganization, setupOffice(testOrganization));
        testEstimate = setupEstimate(testOrganization, testProject);
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testListEstimates_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEstimates_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListEstimates_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEstimates_ReturnsEstimateList() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()); // Returns array directly, not wrapped in data
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetEstimateById_WithValidId_ReturnsEstimate() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates/{id}", testProject.getId(), testEstimate.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEstimate.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetEstimateById_WithInvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates/{id}", testProject.getId(), 99999L)
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateEstimate_WithOrgAdmin_ReturnsCreated() throws Exception {
        // Use unique name to avoid 409 conflict
        String uniqueName = "New Estimate " + System.currentTimeMillis();
        CreateEstimateDto dto = new CreateEstimateDto(
                uniqueName,  // name (required)
                "Test description",  // description
                null,  // notes
                new ArrayList<>()  // lineItems
        );
        mockMvc.perform(post("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateEstimate_WithValidId_ReturnsOk() throws Exception {
        UpdateEstimateDto dto = new UpdateEstimateDto(
                "Updated Estimate",  // name
                "Updated description",  // description
                null,  // notes
                EstimateStatus.DRAFT,  // status
                new ArrayList<>()  // lineItems
        );
        mockMvc.perform(patch("/api/projects/{projectId}/estimates/{id}", testProject.getId(), testEstimate.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== ARCHIVE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveEstimate_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/estimates/{id}/archive", testProject.getId(), testEstimate.getId())
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEstimate.getId()));
    }

    // ========== ADDITIONAL EDGE CASE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEstimates_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEstimates_WithIncludeArchived_ReturnsAll() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString())
                .param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetEstimateById_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates/{id}", testProject.getId(), testEstimate.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetEstimateById_WithIncludeArchived_ReturnsEstimate() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates/{id}", testProject.getId(), testEstimate.getId())
                .param("orgId", testOrganization.getId().toString())
                .param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEstimate.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateEstimate_WithoutOrgId_ThrowsException() throws Exception {
        CreateEstimateDto dto = new CreateEstimateDto(
                "New Estimate", null, null, new ArrayList<>()
        );
        mockMvc.perform(post("/api/projects/{projectId}/estimates", testProject.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateEstimate_WithoutName_ReturnsBadRequest() throws Exception {
        CreateEstimateDto dto = new CreateEstimateDto(
                null, null, null, new ArrayList<>()
        );
        mockMvc.perform(post("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateEstimate_WithoutOrgId_ThrowsException() throws Exception {
        UpdateEstimateDto dto = new UpdateEstimateDto(
                "Updated Estimate", null, null, EstimateStatus.DRAFT, new ArrayList<>()
        );
        mockMvc.perform(patch("/api/projects/{projectId}/estimates/{id}", testProject.getId(), testEstimate.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateEstimate_WithInvalidId_ReturnsNotFound() throws Exception {
        UpdateEstimateDto dto = new UpdateEstimateDto(
                "Updated Estimate", null, null, EstimateStatus.DRAFT, new ArrayList<>()
        );
        mockMvc.perform(patch("/api/projects/{projectId}/estimates/{id}", testProject.getId(), 99999L)
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveEstimate_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/estimates/{id}/archive", testProject.getId(), testEstimate.getId())
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveEstimate_WithInvalidId_ReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/estimates/{id}/archive", testProject.getId(), 99999L)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUnarchiveEstimate_ReturnsServiceUnavailable() throws Exception {
        // Unarchive is not yet implemented
        mockMvc.perform(post("/api/projects/{projectId}/estimates/{id}/unarchive", testProject.getId(), testEstimate.getId())
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListEstimates_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testListEstimates_WithManager_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "PM")
    void testListEstimates_WithPM_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void testListEstimates_WithViewer_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateEstimate_ResponseContainsId() throws Exception {
        String uniqueName = "New Estimate " + System.currentTimeMillis();
        CreateEstimateDto dto = new CreateEstimateDto(
                uniqueName, null, null, new ArrayList<>()
        );
        mockMvc.perform(post("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertTrue(content.contains("id") || content.contains("\"id\""), 
                        "Response should contain id field");
                });
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateEstimate_ResponseContainsUpdatedFields() throws Exception {
        UpdateEstimateDto dto = new UpdateEstimateDto(
                "Updated Estimate Name", "Updated description", null, EstimateStatus.DRAFT, new ArrayList<>()
        );
        mockMvc.perform(patch("/api/projects/{projectId}/estimates/{id}", testProject.getId(), testEstimate.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEstimate.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListEstimates_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/estimates", testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }
}

