package com.mytegroup.api.controller.migrations;

import com.mytegroup.api.dto.migrations.*;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

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
}

