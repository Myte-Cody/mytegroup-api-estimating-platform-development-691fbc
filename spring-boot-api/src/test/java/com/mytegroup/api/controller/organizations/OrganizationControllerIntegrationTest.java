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

    // ========== ADDITIONAL CREATE TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testCreateOrganization_WithoutName_ReturnsBadRequest() throws Exception {
        CreateOrganizationDto dto = new CreateOrganizationDto(
            null, null, null, null, null, null, null, null, null, null, null
        );
        
        mockMvc.perform(post("/api/organizations")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testCreateOrganization_WithUser_IsForbidden() throws Exception {
        CreateOrganizationDto dto = new CreateOrganizationDto(
            "New Organization", null, null, null, null, null, null, null, null, null, null
        );
        
        mockMvc.perform(post("/api/organizations")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateOrganization_WithoutAuthentication_Returns401() throws Exception {
        CreateOrganizationDto dto = new CreateOrganizationDto(
            "New Organization", null, null, null, null, null, null, null, null, null, null
        );
        
        mockMvc.perform(post("/api/organizations")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL LIST TESTS ==========

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testListOrganizations_WithPlatformAdmin_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testListOrganizations_WithPagination_ReturnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/organizations")
                .param("page", "0")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testListOrganizations_WithIncludeArchived_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/organizations")
                .param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListOrganizations_WithUser_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testListOrganizations_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL GET BY ID TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetOrganizationById_WithSuperAdmin_ReturnsOrganization() throws Exception {
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testGetOrganizationById_WithOrgOwner_ReturnsOrganization() throws Exception {
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testGetOrganizationById_WithAdmin_ReturnsOrganization() throws Exception {
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testGetOrganizationById_WithPlatformAdmin_ReturnsOrganization() throws Exception {
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testGetOrganizationById_WithUser_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testGetOrganizationById_NonExistent_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/organizations/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetOrganizationById_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL UPDATE TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testUpdateOrganization_WithSuperAdmin_ReturnsUpdated() throws Exception {
        UpdateOrganizationDto dto = new UpdateOrganizationDto(
            "Super Admin Updated Organization",
            null
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testUpdateOrganization_WithOrgOwner_ReturnsUpdated() throws Exception {
        UpdateOrganizationDto dto = new UpdateOrganizationDto(
            "Org Owner Updated Organization",
            null
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testUpdateOrganization_WithAdmin_ReturnsUpdated() throws Exception {
        UpdateOrganizationDto dto = new UpdateOrganizationDto(
            "Admin Updated Organization",
            null
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testUpdateOrganization_WithUser_IsForbidden() throws Exception {
        UpdateOrganizationDto dto = new UpdateOrganizationDto(
            "User Updated Organization",
            null
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testUpdateOrganization_NonExistent_ReturnsNotFound() throws Exception {
        UpdateOrganizationDto dto = new UpdateOrganizationDto(
            "Updated Organization",
            null
        );
        
        mockMvc.perform(patch("/api/organizations/99999")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateOrganization_WithoutAuthentication_Returns401() throws Exception {
        UpdateOrganizationDto dto = new UpdateOrganizationDto(
            "Updated Organization",
            null
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL ARCHIVE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testArchiveOrganization_WithUser_IsForbidden() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testArchiveOrganization_NonExistent_ReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/organizations/99999/archive")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testArchiveOrganization_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== UNARCHIVE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testUnarchiveOrganization_WithSuperAdmin_ReturnsUnarchived() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/unarchive")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrganization.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUnarchiveOrganization_WithOrgAdmin_IsForbidden() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/unarchive")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testUnarchiveOrganization_WithUser_IsForbidden() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/unarchive")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testUnarchiveOrganization_NonExistent_ReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/organizations/99999/unarchive")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUnarchiveOrganization_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/unarchive")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL DATASTORE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateDatastore_WithOrgAdmin_IsForbidden() throws Exception {
        UpdateOrganizationDatastoreDto dto = new UpdateOrganizationDatastoreDto(
            null, com.mytegroup.api.entity.enums.organization.DatastoreType.DEDICATED,
            "postgresql://localhost:5432/test", null, "testdb", null
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/datastore")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testUpdateDatastore_WithUser_IsForbidden() throws Exception {
        UpdateOrganizationDatastoreDto dto = new UpdateOrganizationDatastoreDto(
            null, com.mytegroup.api.entity.enums.organization.DatastoreType.DEDICATED,
            "postgresql://localhost:5432/test", null, "testdb", null
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/datastore")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testUpdateDatastore_NonExistent_ReturnsNotFound() throws Exception {
        UpdateOrganizationDatastoreDto dto = new UpdateOrganizationDatastoreDto(
            null, com.mytegroup.api.entity.enums.organization.DatastoreType.DEDICATED,
            "postgresql://localhost:5432/test", null, "testdb", null
        );
        
        mockMvc.perform(patch("/api/organizations/99999/datastore")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateDatastore_WithoutAuthentication_Returns401() throws Exception {
        UpdateOrganizationDatastoreDto dto = new UpdateOrganizationDatastoreDto(
            null, com.mytegroup.api.entity.enums.organization.DatastoreType.DEDICATED,
            "postgresql://localhost:5432/test", null, "testdb", null
        );
        
        mockMvc.perform(patch("/api/organizations/" + testOrganization.getId() + "/datastore")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL LEGAL HOLD TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSetLegalHold_WithOrgAdmin_IsForbidden() throws Exception {
        UpdateOrganizationLegalHoldDto dto = new UpdateOrganizationLegalHoldDto(true);
        
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/legal-hold")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testSetLegalHold_WithUser_IsForbidden() throws Exception {
        UpdateOrganizationLegalHoldDto dto = new UpdateOrganizationLegalHoldDto(true);
        
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/legal-hold")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testSetLegalHold_NonExistent_ReturnsNotFound() throws Exception {
        UpdateOrganizationLegalHoldDto dto = new UpdateOrganizationLegalHoldDto(true);
        
        mockMvc.perform(post("/api/organizations/99999/legal-hold")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSetLegalHold_WithoutAuthentication_Returns401() throws Exception {
        UpdateOrganizationLegalHoldDto dto = new UpdateOrganizationLegalHoldDto(true);
        
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/legal-hold")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL PII STRIPPED TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSetPiiStripped_WithOrgAdmin_IsForbidden() throws Exception {
        UpdateOrganizationPiiDto dto = new UpdateOrganizationPiiDto(true);
        
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/pii-stripped")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testSetPiiStripped_WithUser_IsForbidden() throws Exception {
        UpdateOrganizationPiiDto dto = new UpdateOrganizationPiiDto(true);
        
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/pii-stripped")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testSetPiiStripped_NonExistent_ReturnsNotFound() throws Exception {
        UpdateOrganizationPiiDto dto = new UpdateOrganizationPiiDto(true);
        
        mockMvc.perform(post("/api/organizations/99999/pii-stripped")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSetPiiStripped_WithoutAuthentication_Returns401() throws Exception {
        UpdateOrganizationPiiDto dto = new UpdateOrganizationPiiDto(true);
        
        mockMvc.perform(post("/api/organizations/" + testOrganization.getId() + "/pii-stripped")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
