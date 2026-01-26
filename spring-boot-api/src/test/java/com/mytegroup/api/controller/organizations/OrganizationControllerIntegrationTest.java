package com.mytegroup.api.controller.organizations;

import com.mytegroup.api.dto.organizations.CreateOrganizationDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationDatastoreDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationLegalHoldDto;
import com.mytegroup.api.dto.organizations.UpdateOrganizationPiiDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrganizationController.
 * Tests organization operations, RBAC, request/response validation.
 */
class OrganizationControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testCreateOrganization_WithSuperAdmin_ReturnsCreated() throws Exception {
        CreateOrganizationDto dto = new CreateOrganizationDto(
            "New Organization", // name
            null, // metadata
            null, // databaseUri
            null, // datastoreUri
            null, // databaseName
            null, // primaryDomain
            null, // useDedicatedDb
            null, // datastoreType
            null, // dataResidency
            null, // piiStripped
            null  // legalHold
        );
        
        mockMvc.perform(post("/api/organizations")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Organization"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateOrganization_WithOrgAdmin_IsForbidden() throws Exception {
        CreateOrganizationDto dto = new CreateOrganizationDto(
            "New Organization", null, null, null, null, null, null, null, null, null, null
        );
        
        mockMvc.perform(post("/api/organizations")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testListOrganizations_WithSuperAdmin_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListOrganizations_WithOrgAdmin_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isForbidden());
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetOrganizationById_WithOrgAdmin_ReturnsOrganization() throws Exception {
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()))
                .andExpect(jsonPath("$.name").exists());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateOrganization_WithOrgAdmin_ReturnsUpdated() throws Exception {
        UpdateOrganizationDto dto = new UpdateOrganizationDto(
            "Updated Organization", // name
            null  // metadata
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    // ========== ARCHIVE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testArchiveOrganization_WithSuperAdmin_ReturnsArchived() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveOrganization_WithOrgAdmin_IsForbidden() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== UPDATE DATASTORE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testUpdateDatastore_WithSuperAdmin_ReturnsUpdated() throws Exception {
        UpdateOrganizationDatastoreDto dto = new UpdateOrganizationDatastoreDto(
            null, // useDedicatedDb
            com.mytegroup.api.entity.enums.organization.DatastoreType.DEDICATED, // type
            "postgresql://localhost:5432/test", // databaseUri
            null, // datastoreUri
            "testdb", // databaseName
            null  // dataResidency
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/datastore")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    // ========== LEGAL HOLD ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testSetLegalHold_WithSuperAdmin_ReturnsUpdated() throws Exception {
        UpdateOrganizationLegalHoldDto dto = new UpdateOrganizationLegalHoldDto(true);
        
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/legal-hold")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    // ========== PII STRIPPED ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testSetPiiStripped_WithSuperAdmin_ReturnsUpdated() throws Exception {
        UpdateOrganizationPiiDto dto = new UpdateOrganizationPiiDto(true);
        
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/pii-stripped")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }
}
