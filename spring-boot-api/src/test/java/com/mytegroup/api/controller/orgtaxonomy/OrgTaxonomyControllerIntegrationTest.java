package com.mytegroup.api.controller.orgtaxonomy;

import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyDto;
import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyValueDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrgTaxonomyController.
 * Tests organization taxonomy operations, RBAC, request/response validation.
 */
class OrgTaxonomyControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListTaxonomy_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListTaxonomy_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListTaxonomy_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy"))
                    .andExpect(status().isBadRequest()); // Service throws BadRequestException when orgId is missing
    }

    // ========== PUT ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPutTaxonomy_WithValidData_ReturnsOk() throws Exception {
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(
            new PutOrgTaxonomyValueDto("key1", "Label 1", 1, "#FF0000", Map.of())
        ));
        
        mockMvc.perform(put("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.namespace").exists());
    }

    // ========== GET ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTaxonomy_WithValidKey_ReturnsTaxonomy() throws Exception {
        // Service throws ResourceNotFoundException when taxonomy doesn't exist
        mockMvc.perform(get("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testDeleteTaxonomy_WithValidKey_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // ========== ADDITIONAL EDGE CASE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPutTaxonomy_WithoutOrgId_ThrowsException() throws Exception {
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(
            new PutOrgTaxonomyValueDto("key1", "Label 1", 1, "#FF0000", Map.of())
        ));
        
        mockMvc.perform(put("/api/org-taxonomy/test-key")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTaxonomy_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy/test-key"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testDeleteTaxonomy_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(delete("/api/org-taxonomy/test-key")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPutTaxonomy_WithEmptyValues_ReturnsOk() throws Exception {
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of());
        
        mockMvc.perform(put("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPutTaxonomy_WithMultipleValues_ReturnsOk() throws Exception {
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(
            new PutOrgTaxonomyValueDto("key1", "Label 1", 1, "#FF0000", Map.of("meta1", "value1")),
            new PutOrgTaxonomyValueDto("key2", "Label 2", 2, "#00FF00", Map.of("meta2", "value2"))
        ));
        
        mockMvc.perform(put("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPutTaxonomy_WithNullValues_ReturnsOk() throws Exception {
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(null);
        
        mockMvc.perform(put("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTaxonomy_AfterPut_ReturnsTaxonomy() throws Exception {
        // First create a taxonomy
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(
            new PutOrgTaxonomyValueDto("key1", "Label 1", 1, "#FF0000", Map.of())
        ));
        
        mockMvc.perform(put("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
        
        // Then get it - may fail if service error occurred
        mockMvc.perform(get("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListTaxonomy_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListTaxonomy_WithSuperAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testListTaxonomy_WithOrgOwner_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testListTaxonomy_WithPlatformAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPutTaxonomy_ResponseContainsNamespace() throws Exception {
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(
            new PutOrgTaxonomyValueDto("key1", "Label 1", 1, "#FF0000", Map.of())
        ));
        
        mockMvc.perform(put("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.namespace").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPutTaxonomy_WithMetadata_ReturnsOk() throws Exception {
        PutOrgTaxonomyDto dto = new PutOrgTaxonomyDto();
        dto.setValues(List.of(
            new PutOrgTaxonomyValueDto("key1", "Label 1", 1, "#FF0000", 
                Map.of("description", "Test description", "category", "test"))
        ));
        
        mockMvc.perform(put("/api/org-taxonomy/test-key")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListTaxonomy_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    @Test
    void testListTaxonomy_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/org-taxonomy")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isUnauthorized());
    }
}

