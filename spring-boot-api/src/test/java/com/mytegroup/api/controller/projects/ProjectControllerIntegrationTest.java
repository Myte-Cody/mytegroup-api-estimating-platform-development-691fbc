package com.mytegroup.api.controller.projects;

import com.mytegroup.api.dto.projects.CreateProjectDto;
import com.mytegroup.api.dto.projects.UpdateProjectDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.service.projects.ProjectsService;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProjectController.
 * Tests project CRUD operations, RBAC, request/response validation.
 */
class ProjectControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Office testOffice;
    private Project testProject;

    @Autowired
    private ProjectsService projectsService;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testOffice = setupOffice(testOrganization);
        testProject = setupProject(testOrganization, testOffice, "Test Project");
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testListProjects_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetProjectById_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isUnauthorized());
    }

    // ========== RBAC TESTS - ALLOWED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListProjects_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/projects")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testListProjects_WithOrgOwner_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/projects")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListProjects_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/projects")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    // ========== RBAC TESTS - DENIED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListProjects_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/projects")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListProjects_ReturnsProjectList() throws Exception {
        mockMvc.perform(get("/api/projects")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListProjects_WithPagination_ReturnsPaginatedResult() throws Exception {
        mockMvc.perform(get("/api/projects")
                .param("orgId", testOrganization.getId().toString())
                .param("page", "0")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10));
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetProjectById_WithValidId_ReturnsProject() throws Exception {
        mockMvc.perform(get("/api/projects/" + testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testProject.getId()))
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetProjectById_WithInvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/projects/99999")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetProjectById_ResponseHasRequiredFields() throws Exception {
        mockMvc.perform(get("/api/projects/" + testProject.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateProject_WithOrgAdmin_ReturnsCreated() throws Exception {
        CreateProjectDto dto = new CreateProjectDto(
                "New Project",  // name
                "Test description",  // description
                testOrganization.getId().toString(),  // orgId
                null,  // officeId
                "PROJ-001",  // projectCode
                "DRAFT",  // status
                null,  // location
                null,  // bidDate
                null,  // awardDate
                null,  // fabricationStartDate
                null,  // fabricationEndDate
                null,  // erectionStartDate
                null,  // erectionEndDate
                null,  // completionDate
                null,  // budget
                null,  // quantities
                null,  // staffing
                null   // costCodeBudgets
        );
        mockMvc.perform(post("/api/projects")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testCreateProject_WithUserRole_IsForbidden() throws Exception {
        CreateProjectDto dto = new CreateProjectDto(
                "Unauthorized Project",  // name
                null,  // description
                testOrganization.getId().toString(),  // orgId
                null,  // officeId
                null,  // projectCode
                null,  // status
                null,  // location
                null,  // bidDate
                null,  // awardDate
                null,  // fabricationStartDate
                null,  // fabricationEndDate
                null,  // erectionStartDate
                null,  // erectionEndDate
                null,  // completionDate
                null,  // budget
                null,  // quantities
                null,  // staffing
                null   // costCodeBudgets
        );
        mockMvc.perform(post("/api/projects")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateProject_WithValidId_ReturnsOk() throws Exception {
        UpdateProjectDto dto = new UpdateProjectDto(
                "Updated Project",  // name
                "Updated description",  // description
                null,  // officeId
                "UPD-001",  // projectCode
                "ACTIVE",  // status
                null,  // location
                null,  // bidDate
                null,  // awardDate
                null,  // fabricationStartDate
                null,  // fabricationEndDate
                null,  // erectionStartDate
                null,  // erectionEndDate
                null,  // completionDate
                null,  // budget
                null,  // quantities
                null,  // staffing
                null   // costCodeBudgets
        );
        mockMvc.perform(patch("/api/projects/" + testProject.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testUpdateProject_WithUserRole_IsForbidden() throws Exception {
        UpdateProjectDto dto = new UpdateProjectDto(
                "Unauthorized Update",  // name
                null,  // description
                null,  // officeId
                null,  // projectCode
                null,  // status
                null,  // location
                null,  // bidDate
                null,  // awardDate
                null,  // fabricationStartDate
                null,  // fabricationEndDate
                null,  // erectionStartDate
                null,  // erectionEndDate
                null,  // completionDate
                null,  // budget
                null,  // quantities
                null,  // staffing
                null   // costCodeBudgets
        );
        mockMvc.perform(patch("/api/projects/" + testProject.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== ARCHIVE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveProject_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testProject.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testArchiveProject_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== CONTENT TYPE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListProjects_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/projects")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    // ========== ADDITIONAL ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListProjects_WithoutOrgId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetProjectById_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/projects/" + testProject.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateProject_WithoutOrgId_ThrowsException() throws Exception {
        UpdateProjectDto dto = new UpdateProjectDto("Updated", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        mockMvc.perform(patch("/api/projects/" + testProject.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUnarchiveProject_WithValidId_ReturnsOk() throws Exception {
        // First archive the project so we can unarchive it
        projectsService.archive(testProject.getId(), testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/unarchive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testProject.getId()));
    }
}

