package com.mytegroup.api.controller.organizations;

import com.mytegroup.api.dto.organizations.CreateOrganizationDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrganizationController.
 * Tests organization CRUD operations, admin access, RBAC validation.
 */
class OrganizationControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Org");
        testUser = setupUser(testOrganization, "testuser@example.com");
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testListOrganizations_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isUnauthorized());
    }

    // ========== RBAC TESTS - ADMIN ONLY ==========
    // Note: OrganizationController requires SUPER_ADMIN or PLATFORM_ADMIN for list

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListOrganizations_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListOrganizations_WithSuperAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testListOrganizations_WithPlatformAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk());
    }

    // ========== RBAC TESTS - DENIED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListOrganizations_WithOrgAdmin_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListOrganizations_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListOrganizations_ReturnsOrgList() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListOrganizations_WithPagination_ReturnsPaginatedResult() throws Exception {
        mockMvc.perform(get("/api/organizations")
                .param("page", "0")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10));
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testGetOrganizationById_WithValidId_ReturnsOrg() throws Exception {
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()))
                .andExpect(jsonPath("$.name").value("Test Org"));
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testGetOrganizationById_WithInvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/organizations/99999"))
                .andExpect(status().isNotFound());
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testCreateOrganization_WithAdmin_ReturnsCreated() throws Exception {
        CreateOrganizationDto dto = new CreateOrganizationDto(
                "New Organization",  // name (required)
                null,  // metadata
                null,  // databaseUri
                null,  // datastoreUri
                null,  // databaseName
                null,  // primaryDomain
                null,  // useDedicatedDb
                null,  // datastoreType
                null,  // dataResidency
                null,  // piiStripped
                null   // legalHold
        );
        mockMvc.perform(post("/api/organizations")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateOrganization_WithOrgAdmin_IsForbidden() throws Exception {
        // Need valid DTO body to pass validation before authorization check
        CreateOrganizationDto dto = new CreateOrganizationDto(
                "New Organization",  // name (required)
                null,  // metadata
                null,  // databaseUri
                null,  // datastoreUri
                null,  // databaseName
                null,  // primaryDomain
                null,  // useDedicatedDb
                null,  // datastoreType
                null,  // dataResidency
                null,  // piiStripped
                null   // legalHold
        );
        mockMvc.perform(post("/api/organizations")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testUpdateOrganization_WithValidId_ReturnsOk() throws Exception {
        UpdateOrganizationDto dto = new UpdateOrganizationDto(
                "Updated Organization",  // name
                null  // metadata
        );
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== SET OWNER ENDPOINT TESTS ==========
    // Note: OrganizationController does not have a setOwner endpoint

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    @org.junit.jupiter.api.Disabled("OrganizationController does not have a setOwner endpoint")
    void testSetOwner_WithValidIds_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/owner")
                .param("userId", testUser.getId().toString())
                .contentType(APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== ARCHIVE ENDPOINT TESTS ==========
    // Note: OrganizationController uses PATCH, not POST

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testArchiveOrganization_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== CONTENT TYPE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListOrganizations_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }
}

