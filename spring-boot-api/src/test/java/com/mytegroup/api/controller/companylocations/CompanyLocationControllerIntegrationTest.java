package com.mytegroup.api.controller.companylocations;

import com.mytegroup.api.dto.companylocations.CreateCompanyLocationDto;
import com.mytegroup.api.dto.companylocations.UpdateCompanyLocationDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.service.companylocations.CompanyLocationsService;
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
 * Integration tests for CompanyLocationController.
 * Tests company location operations, RBAC, request/response validation.
 */
class CompanyLocationControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Company testCompany;
    private CompanyLocation testLocation;

    @Autowired
    private CompanyLocationsService companyLocationsService;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testCompany = setupCompany(testOrganization, "Test Company");
        testLocation = setupCompanyLocation(testCompany, "Test Location");
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListLocations_WithOrgAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/company-locations")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListLocations_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/company-locations")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListLocations_WithoutOrgId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/company-locations"))
                .andExpect(status().isBadRequest());
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateLocation_WithValidData_ReturnsCreated() throws Exception {
        CreateCompanyLocationDto dto = new CreateCompanyLocationDto(
            testCompany.getId().toString(), // companyId
            "New Location", // name
            null, // externalId
            null, // timezone
            null, // email
            null, // phone
            null, // addressLine1
            null, // addressLine2
            null, // city
            null, // region
            null, // postal
            null, // country
            null, // tagKeys
            null  // notes
        );
        
        mockMvc.perform(post("/api/company-locations")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateLocation_WithoutOrgId_ThrowsException() throws Exception {
        CreateCompanyLocationDto dto = new CreateCompanyLocationDto(
            testCompany.getId().toString(), "New Location", null, null, null, null, null, null, null, null, null, null, null, null
        );
        
        mockMvc.perform(post("/api/company-locations")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetLocationById_WithValidId_ReturnsLocation() throws Exception {
        mockMvc.perform(get("/api/company-locations/" + testLocation.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLocation.getId()));
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateLocation_WithValidData_ReturnsUpdated() throws Exception {
        UpdateCompanyLocationDto dto = new UpdateCompanyLocationDto(
            "Updated Location", // name
            null, // externalId
            null, // timezone
            null, // email
            null, // phone
            null, // addressLine1
            null, // addressLine2
            null, // city
            null, // region
            null, // postal
            null, // country
            null, // tagKeys
            null  // notes
        );
        
        mockMvc.perform(patch("/api/company-locations/" + testLocation.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLocation.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateLocation_WithoutOrgId_ThrowsException() throws Exception {
        UpdateCompanyLocationDto dto = new UpdateCompanyLocationDto(
            "Updated", null, null, null, null, null, null, null, null, null, null, null, null
        );
        
        mockMvc.perform(patch("/api/company-locations/" + testLocation.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ARCHIVE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveLocation_WithValidId_ReturnsOk() throws Exception {
        // Archive should succeed even if audit log fails (audit log catches exceptions)
        mockMvc.perform(post("/api/company-locations/" + testLocation.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLocation.getId()))
                .andExpect(jsonPath("$.companyId").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveLocation_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(post("/api/company-locations/" + testLocation.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== UNARCHIVE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUnarchiveLocation_WithValidId_ReturnsOk() throws Exception {
        // First archive the location so we can unarchive it
        companyLocationsService.archive(testLocation.getId(), testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/company-locations/" + testLocation.getId() + "/unarchive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLocation.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUnarchiveLocation_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(post("/api/company-locations/" + testLocation.getId() + "/unarchive")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}

