package com.mytegroup.api.controller.migrations;

import com.mytegroup.api.dto.migrations.*;
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
 * Integration tests for MigrationController.
 * Tests migration operations, RBAC, request/response validation.
 */
class MigrationControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testStartMigration_WithSuperAdmin_IsAllowed() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        dto.setTargetDatastoreType("POSTGRESQL");
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testStartMigration_WithOrgAdmin_IsForbidden() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        dto.setTargetDatastoreType("POSTGRESQL");
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== STATUS ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetMigrationStatus_WithSuperAdmin_ReturnsStatus() throws Exception {
        mockMvc.perform(get("/api/migration/status/" + testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    // ========== ABORT ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testAbortMigration_WithSuperAdmin_ReturnsOk() throws Exception {
        AbortMigrationDto dto = new AbortMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/abort")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    // ========== FINALIZE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testFinalizeMigration_WithSuperAdmin_ReturnsOk() throws Exception {
        FinalizeMigrationDto dto = new FinalizeMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/finalize")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    // ========== ADDITIONAL START TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testStartMigration_WithUser_IsForbidden() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        dto.setTargetDatastoreType("POSTGRESQL");
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testStartMigration_WithAdmin_IsForbidden() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        dto.setTargetDatastoreType("POSTGRESQL");
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testStartMigration_WithOrgOwner_IsForbidden() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        dto.setTargetDatastoreType("POSTGRESQL");
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testStartMigration_WithoutAuthentication_Returns401() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        dto.setTargetDatastoreType("POSTGRESQL");
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testStartMigration_WithoutOrgId_ReturnsBadRequest() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setTargetDatastoreType("POSTGRESQL");
        // orgId is null
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testStartMigration_WithoutTargetType_ReturnsOkOrBadRequest() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        // targetDatastoreType is null - service may handle this gracefully
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 200 || status == 400, 
                        "Expected status 200 or 400 but got " + status);
                });
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testStartMigration_ResponseContainsStatus() throws Exception {
        StartMigrationDto dto = new StartMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        dto.setTargetDatastoreType("POSTGRESQL");
        
        mockMvc.perform(post("/api/migration/start")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    // ========== ADDITIONAL STATUS TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetMigrationStatus_WithOrgAdmin_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/migration/status/" + testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testGetMigrationStatus_WithUser_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/migration/status/" + testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testGetMigrationStatus_WithAdmin_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/migration/status/" + testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetMigrationStatus_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/migration/status/" + testOrganization.getId().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetMigrationStatus_ResponseContainsStatusField() throws Exception {
        mockMvc.perform(get("/api/migration/status/" + testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetMigrationStatus_WithNonExistentOrg_ReturnsStatus() throws Exception {
        // Should return status even if org doesn't exist (might be "not_found" or similar)
        mockMvc.perform(get("/api/migration/status/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    // ========== ADDITIONAL ABORT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAbortMigration_WithOrgAdmin_IsForbidden() throws Exception {
        AbortMigrationDto dto = new AbortMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/abort")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testAbortMigration_WithUser_IsForbidden() throws Exception {
        AbortMigrationDto dto = new AbortMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/abort")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testAbortMigration_WithAdmin_IsForbidden() throws Exception {
        AbortMigrationDto dto = new AbortMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/abort")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAbortMigration_WithoutAuthentication_Returns401() throws Exception {
        AbortMigrationDto dto = new AbortMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/abort")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testAbortMigration_WithoutOrgId_ReturnsBadRequest() throws Exception {
        AbortMigrationDto dto = new AbortMigrationDto();
        // orgId is null
        
        mockMvc.perform(post("/api/migration/abort")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testAbortMigration_ResponseContainsStatus() throws Exception {
        AbortMigrationDto dto = new AbortMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/abort")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    // ========== ADDITIONAL FINALIZE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testFinalizeMigration_WithOrgAdmin_IsForbidden() throws Exception {
        FinalizeMigrationDto dto = new FinalizeMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/finalize")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testFinalizeMigration_WithUser_IsForbidden() throws Exception {
        FinalizeMigrationDto dto = new FinalizeMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/finalize")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testFinalizeMigration_WithAdmin_IsForbidden() throws Exception {
        FinalizeMigrationDto dto = new FinalizeMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/finalize")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFinalizeMigration_WithoutAuthentication_Returns401() throws Exception {
        FinalizeMigrationDto dto = new FinalizeMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/finalize")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testFinalizeMigration_WithoutOrgId_ReturnsBadRequest() throws Exception {
        FinalizeMigrationDto dto = new FinalizeMigrationDto();
        // orgId is null
        
        mockMvc.perform(post("/api/migration/finalize")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testFinalizeMigration_ResponseContainsStatus() throws Exception {
        FinalizeMigrationDto dto = new FinalizeMigrationDto();
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/migration/finalize")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }
}

