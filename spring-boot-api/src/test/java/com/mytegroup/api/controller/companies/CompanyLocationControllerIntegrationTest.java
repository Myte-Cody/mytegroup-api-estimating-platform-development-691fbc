package com.mytegroup.api.controller.companies;

import com.mytegroup.api.dto.companylocations.CreateCompanyLocationDto;
import com.mytegroup.api.dto.companylocations.UpdateCompanyLocationDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.HashMap;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CompanyLocationController.
 * Tests company location CRUD operations, RBAC, request/response validation.
 */
class CompanyLocationControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Company testCompany;
    private CompanyLocation testLocation;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testCompany = setupCompany(testOrganization, "Test Company");
        testLocation = setupCompanyLocation(testCompany, "Main Location");
    }

    @Test
    void testListCompanyLocations_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/company-locations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCompanyLocations_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/company-locations")
                .param("orgId", testOrganization.getId().toString())
                .param("companyId", testCompany.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListCompanyLocations_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/company-locations")
                .param("orgId", testOrganization.getId().toString())
                .param("companyId", testCompany.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCompanyLocations_ReturnsLocationList() throws Exception {
        mockMvc.perform(get("/api/company-locations")
                .param("orgId", testOrganization.getId().toString())
                .param("companyId", testCompany.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetCompanyLocationById_WithValidId_ReturnsLocation() throws Exception {
        mockMvc.perform(get("/api/company-locations/" + testLocation.getId())
                .param("orgId", testOrganization.getId().toString())
                .param("companyId", testCompany.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLocation.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateCompanyLocation_WithOrgAdmin_ReturnsCreated() throws Exception {
        CreateCompanyLocationDto dto = new CreateCompanyLocationDto(
                testCompany.getId().toString(),  // companyId (required)
                "New Location",  // name (required)
                null,  // externalId
                "America/New_York",  // timezone
                "location@test.com",  // email
                "+1234567890",  // phone
                "123 Main St",  // addressLine1
                null,  // addressLine2
                "New York",  // city
                "NY",  // region
                "10001",  // postal
                "US",  // country
                new ArrayList<>(),  // tagKeys
                null   // notes
        );
        mockMvc.perform(post("/api/company-locations")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateCompanyLocation_WithValidId_ReturnsOk() throws Exception {
        UpdateCompanyLocationDto dto = new UpdateCompanyLocationDto(
                "Updated Location",  // name
                null,  // externalId
                null,  // timezone
                "updated@test.com",  // email
                null,  // phone
                null,  // addressLine1
                null,  // addressLine2
                null,  // city
                null,  // region
                null,  // postal
                null,  // country
                null,  // tagKeys
                null   // notes
        );
        mockMvc.perform(patch("/api/company-locations/" + testLocation.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCompanyLocations_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/company-locations")
                .param("orgId", testOrganization.getId().toString())
                .param("companyId", testCompany.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }
}

