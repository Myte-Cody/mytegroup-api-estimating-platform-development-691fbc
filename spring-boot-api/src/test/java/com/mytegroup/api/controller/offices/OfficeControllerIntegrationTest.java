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
                .andExpect(status().isOk());
    }
}

