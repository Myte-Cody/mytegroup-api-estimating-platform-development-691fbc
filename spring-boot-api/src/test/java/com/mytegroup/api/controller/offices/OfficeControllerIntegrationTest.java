package com.mytegroup.api.controller.offices;

import com.mytegroup.api.dto.offices.CreateOfficeDto;
import com.mytegroup.api.dto.offices.UpdateOfficeDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OfficeController.
 * Tests office CRUD operations, RBAC, request/response validation.
 */
class OfficeControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Office testOffice;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testOffice = setupOffice(testOrganization, "Main Office");
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListOffices_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/offices")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListOffices_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/offices")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetOfficeById_WithValidId_ReturnsOffice() throws Exception {
        mockMvc.perform(get("/api/offices/" + testOffice.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetOfficeById_WithInvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/offices/99999")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateOffice_WithOrgAdmin_ReturnsCreated() throws Exception {
        CreateOfficeDto dto = new CreateOfficeDto(
                "New Office",  // name (required)
                null,  // orgId
                "123 Test St",  // address
                "Test office description",  // description
                "America/New_York",  // timezone
                null,  // orgLocationTypeKey
                null,  // tagKeys
                null,  // parentOrgLocationId
                0  // sortOrder
        );
        mockMvc.perform(post("/api/offices")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateOffice_WithValidId_ReturnsOk() throws Exception {
        UpdateOfficeDto dto = new UpdateOfficeDto(
                "Updated Office",  // name
                null,  // address
                null,  // description
                null,  // timezone
                null,  // orgLocationTypeKey
                null,  // tagKeys
                null,  // parentOrgLocationId
                null  // sortOrder
        );
        mockMvc.perform(patch("/api/offices/" + testOffice.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOffice.getId()))
                .andExpect(jsonPath("$.name").value("Updated Office"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListOffices_WithoutOrgId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/offices"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListOffices_WithIncludeArchived_ReturnsAll() throws Exception {
        mockMvc.perform(get("/api/offices")
                .param("orgId", testOrganization.getId().toString())
                .param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateOffice_WithoutOrgId_ReturnsBadRequest() throws Exception {
        CreateOfficeDto dto = new CreateOfficeDto(
                "New Office", null, null, null, null, null, null, null, 0
        );
        mockMvc.perform(post("/api/offices")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testCreateOffice_WithUserRole_IsForbidden() throws Exception {
        CreateOfficeDto dto = new CreateOfficeDto(
                "New Office", null, null, null, null, null, null, null, 0
        );
        mockMvc.perform(post("/api/offices")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetOfficeById_WithoutOrgId_ReturnsOk() throws Exception {
        // OfficeController.getById doesn't require orgId - service allows null orgId
        // Service will return office if found, regardless of orgId
        mockMvc.perform(get("/api/offices/" + testOffice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOffice.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testGetOfficeById_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/offices/" + testOffice.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateOffice_WithoutOrgId_ReturnsBadRequest() throws Exception {
        UpdateOfficeDto dto = new UpdateOfficeDto(
                "Updated", null, null, null, null, null, null, null
        );
        mockMvc.perform(patch("/api/offices/" + testOffice.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testUpdateOffice_WithUserRole_IsForbidden() throws Exception {
        UpdateOfficeDto dto = new UpdateOfficeDto(
                "Updated", null, null, null, null, null, null, null
        );
        mockMvc.perform(patch("/api/offices/" + testOffice.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveOffice_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/offices/" + testOffice.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOffice.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveOffice_WithoutOrgId_ReturnsOk() throws Exception {
        // OfficeController.archive doesn't require orgId - service allows null orgId
        // Service will archive office if found, regardless of orgId
        mockMvc.perform(post("/api/offices/" + testOffice.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOffice.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testArchiveOffice_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/offices/" + testOffice.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUnarchiveOffice_WithValidId_ReturnsOk() throws Exception {
        // First archive the office
        mockMvc.perform(post("/api/offices/" + testOffice.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk());

        // Then unarchive it
        mockMvc.perform(post("/api/offices/" + testOffice.getId() + "/unarchive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOffice.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUnarchiveOffice_WithoutOrgId_ReturnsOk() throws Exception {
        // First archive the office
        mockMvc.perform(post("/api/offices/" + testOffice.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isOk());
        
        // OfficeController.unarchive doesn't require orgId - service allows null orgId
        // Service will unarchive office if found, regardless of orgId
        mockMvc.perform(post("/api/offices/" + testOffice.getId() + "/unarchive")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOffice.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testUnarchiveOffice_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/offices/" + testOffice.getId() + "/unarchive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testListOffices_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/offices")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListOffices_WithPagination_ReturnsPaginated() throws Exception {
        mockMvc.perform(get("/api/offices")
                .param("orgId", testOrganization.getId().toString())
                .param("page", "0")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10));
    }
}

