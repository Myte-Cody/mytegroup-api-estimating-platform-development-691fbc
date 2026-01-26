package com.mytegroup.api.controller.costcodes;

import com.mytegroup.api.dto.costcodes.CreateCostCodeDto;
import com.mytegroup.api.dto.costcodes.UpdateCostCodeDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CostCodeController.
 * Tests cost code CRUD operations, RBAC, request/response validation.
 */
class CostCodeControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private CostCode testCostCode;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testCostCode = setupCostCode(testOrganization, "LABOR");
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testListCostCodes_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/cost-codes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetCostCodeById_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/cost-codes/1"))
                .andExpect(status().isUnauthorized());
    }

    // ========== RBAC TESTS - ALLOWED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCostCodes_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testListCostCodes_WithOrgOwner_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    // ========== RBAC TESTS - DENIED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListCostCodes_WithUserRole_IsAllowed() throws Exception {
        // CostCodeController GET list endpoint doesn't have @PreAuthorize annotation,
        // so ROLE_USER can access it (only requires authentication via class-level @PreAuthorize)
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCostCodes_ReturnsCostCodeList() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCostCodes_WithPagination_ReturnsPaginatedResult() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString())
                .param("page", "0")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetCostCodeById_WithValidId_ReturnsCostCode() throws Exception {
        mockMvc.perform(get("/api/cost-codes/" + testCostCode.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCostCode.getId()))
                .andExpect(jsonPath("$.code").value("LABOR"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetCostCodeById_WithInvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/cost-codes/99999")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateCostCode_WithOrgAdmin_ReturnsCreated() throws Exception {
        CreateCostCodeDto dto = new CreateCostCodeDto(
                "Labor",  // category (required)
                "LAB-001",  // code (required)
                "Labor cost code description"  // description (required)
        );
        mockMvc.perform(post("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testCreateCostCode_WithUserRole_IsForbidden() throws Exception {
        CreateCostCodeDto dto = new CreateCostCodeDto(
                "Labor",
                "LAB-001",
                "Labor cost code description"
        );
        mockMvc.perform(post("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateCostCode_WithValidId_ReturnsOk() throws Exception {
        UpdateCostCodeDto dto = new UpdateCostCodeDto(
                "Updated Category",  // category
                "UPD-001",  // code
                "Updated description"  // description
        );
        mockMvc.perform(patch("/api/cost-codes/" + testCostCode.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== CONTENT TYPE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCostCodes_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    // ========== TOGGLE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testToggleCostCode_WithValidId_ReturnsToggled() throws Exception {
        com.mytegroup.api.dto.costcodes.ToggleCostCodeDto dto = 
            new com.mytegroup.api.dto.costcodes.ToggleCostCodeDto(false);
        
        mockMvc.perform(post("/api/cost-codes/" + testCostCode.getId() + "/toggle")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCostCode.getId()));
    }

    // ========== BULK ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testBulkCostCodes_WithOrgAdmin_ReturnsNotImplemented() throws Exception {
        // Create a valid CostCodeInputDto for the bulk operation
        com.mytegroup.api.dto.costcodes.CostCodeInputDto codeDto = 
            new com.mytegroup.api.dto.costcodes.CostCodeInputDto("Labor", "LAB-001", "Description");
        com.mytegroup.api.dto.costcodes.BulkCostCodesDto dto = 
            new com.mytegroup.api.dto.costcodes.BulkCostCodesDto(java.util.List.of(codeDto));
        
        mockMvc.perform(post("/api/cost-codes/bulk")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("Bulk create not yet implemented"));
    }

    // ========== SEED ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSeedCostCodes_WithOrgAdmin_ReturnsNotImplemented() throws Exception {
        com.mytegroup.api.dto.costcodes.SeedCostCodesDto dto = 
            new com.mytegroup.api.dto.costcodes.SeedCostCodesDto("standard", null);
        
        mockMvc.perform(post("/api/cost-codes/seed")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("Seed defaults not yet implemented"));
    }

    // ========== ADDITIONAL LIST TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCostCodes_WithSearchQuery_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString())
                .param("q", "LABOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCostCodes_WithActiveOnly_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString())
                .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListCostCodes_WithoutOrgId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/cost-codes"))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL GET BY ID TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetCostCodeById_WithoutOrgId_ReturnsCostCode() throws Exception {
        // getById works without orgId (orgId is optional), so it should return 200 OK
        mockMvc.perform(get("/api/cost-codes/" + testCostCode.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCostCode.getId()));
    }

    // ========== ADDITIONAL CREATE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateCostCode_WithoutOrgId_ThrowsException() throws Exception {
        CreateCostCodeDto dto = new CreateCostCodeDto(
                "Labor", "LAB-001", "Labor cost code description"
        );
        mockMvc.perform(post("/api/cost-codes")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL UPDATE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateCostCode_WithoutOrgId_ThrowsException() throws Exception {
        UpdateCostCodeDto dto = new UpdateCostCodeDto(
                "Updated Category", "UPD-001", "Updated description"
        );
        mockMvc.perform(patch("/api/cost-codes/" + testCostCode.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL TOGGLE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testToggleCostCode_WithoutOrgId_ThrowsException() throws Exception {
        com.mytegroup.api.dto.costcodes.ToggleCostCodeDto dto = 
            new com.mytegroup.api.dto.costcodes.ToggleCostCodeDto(true);
        
        mockMvc.perform(post("/api/cost-codes/" + testCostCode.getId() + "/toggle")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== IMPORT PREVIEW TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testImportPreview_WithOrgAdmin_ReturnsNotImplemented() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = 
            new org.springframework.mock.web.MockMultipartFile(
                "file", "costcodes.csv", "text/csv", "category,code,description\nLabor,LAB-001,Test".getBytes()
            );
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/cost-codes/import/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("Import preview not yet implemented"));
    }

    // ========== IMPORT COMMIT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testImportCommit_WithOrgAdmin_ReturnsNotImplemented() throws Exception {
        com.mytegroup.api.dto.costcodes.CostCodeImportCommitDto dto = 
            new com.mytegroup.api.dto.costcodes.CostCodeImportCommitDto(java.util.List.of());
        
        mockMvc.perform(post("/api/cost-codes/import/commit")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("Import commit not yet implemented"));
    }

    // ========== ADDITIONAL RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListCostCodes_WithSuperAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testListCostCodes_WithPlatformAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/cost-codes")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }
}

