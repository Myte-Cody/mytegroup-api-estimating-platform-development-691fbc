package com.mytegroup.api.controller.legal;

import com.mytegroup.api.dto.legal.AcceptLegalDocDto;
import com.mytegroup.api.dto.legal.CreateLegalDocDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.legal.LegalDocType;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LegalController.
 * Tests legal document operations, RBAC, request/response validation.
 */
class LegalControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
    }

    // ========== AUTHENTICATION TESTS ==========
    // Note: Unauthenticated tests may fail due to JWT filter configuration
    // Skipping for now as security configuration may throw exceptions

    // ========== LIST DOCS ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListDocs_WithOrgAdmin_ReturnsDocs() throws Exception {
        mockMvc.perform(get("/api/legal/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListDocs_WithTypeFilter_ReturnsFilteredDocs() throws Exception {
        mockMvc.perform(get("/api/legal/docs")
                .param("type", "PRIVACY_POLICY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListDocs_WithCurrentOnly_ReturnsCurrentDocs() throws Exception {
        mockMvc.perform(get("/api/legal/docs")
                .param("currentOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ========== GET DOC BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetDoc_WithValidId_ReturnsNotImplemented() throws Exception {
        // This endpoint throws UnsupportedOperationException
        mockMvc.perform(get("/api/legal/docs/1"))
                .andExpect(status().is5xxServerError());
    }

    // ========== CREATE DOC ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testCreateDoc_WithSuperAdmin_ReturnsCreated() throws Exception {
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType(LegalDocType.PRIVACY_POLICY.getValue());
        dto.setVersion("1.0");
        dto.setTitle("Test Privacy Policy");
        dto.setContent("Test content for privacy policy that is long enough to meet validation requirements");
        // Set effectiveAt to null to avoid LocalDateTime serialization issues
        // The service will handle default values
        dto.setEffectiveAt(null);
        
        String jsonContent = "{\"type\":\"" + LegalDocType.PRIVACY_POLICY.getValue() + 
                            "\",\"version\":\"1.0\",\"title\":\"Test Privacy Policy\"," +
                            "\"content\":\"Test content for privacy policy that is long enough to meet validation requirements\"}";
        
        mockMvc.perform(post("/api/legal/docs")
                .contentType(APPLICATION_JSON)
                .content(jsonContent)
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateDoc_WithOrgAdmin_IsForbidden() throws Exception {
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType(LegalDocType.PRIVACY_POLICY.getValue());
        dto.setVersion("1.0");
        dto.setTitle("Test Privacy Policy");
        dto.setContent("Test content for privacy policy that is long enough to meet validation requirements");
        // Set effectiveAt to null to avoid LocalDateTime serialization issues
        dto.setEffectiveAt(null);
        
        String jsonContent = "{\"type\":\"" + LegalDocType.PRIVACY_POLICY.getValue() + 
                            "\",\"version\":\"1.0\",\"title\":\"Test Privacy Policy\"," +
                            "\"content\":\"Test content for privacy policy that is long enough to meet validation requirements\"}";
        
        mockMvc.perform(post("/api/legal/docs")
                .contentType(APPLICATION_JSON)
                .content(jsonContent)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== ACCEPT ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAccept_WithValidDto_ReturnsNotImplemented() throws Exception {
        AcceptLegalDocDto dto = new AcceptLegalDocDto();
        dto.setDocId(1L);
        dto.setOrgId(testOrganization.getId().toString());
        
        // This endpoint throws UnsupportedOperationException
        mockMvc.perform(post("/api/legal/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== ACCEPTANCE STATUS ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAcceptanceStatus_WithValidParams_ReturnsNotImplemented() throws Exception {
        // This endpoint throws UnsupportedOperationException
        mockMvc.perform(get("/api/legal/acceptance-status")
                .param("docType", "PRIVACY_POLICY")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().is5xxServerError());
    }

    // ========== ADDITIONAL LIST DOCS TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListDocs_WithUser_ReturnsDocs() throws Exception {
        mockMvc.perform(get("/api/legal/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testListDocs_WithSuperAdmin_ReturnsDocs() throws Exception {
        mockMvc.perform(get("/api/legal/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListDocs_WithAdmin_ReturnsDocs() throws Exception {
        mockMvc.perform(get("/api/legal/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testListDocs_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/legal/docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListDocs_WithMultipleFilters_ReturnsFilteredDocs() throws Exception {
        mockMvc.perform(get("/api/legal/docs")
                .param("type", "TERMS")
                .param("currentOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListDocs_WithCurrentOnlyFalse_ReturnsAllDocs() throws Exception {
        mockMvc.perform(get("/api/legal/docs")
                .param("currentOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ========== ADDITIONAL GET DOC BY ID TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testGetDoc_WithUser_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/legal/docs/1"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetDoc_WithSuperAdmin_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/legal/docs/1"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testGetDoc_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/legal/docs/1"))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL CREATE DOC TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testCreateDoc_WithTerms_ReturnsCreated() throws Exception {
        String jsonContent = "{\"type\":\"TERMS\"," +
                            "\"version\":\"1.0\",\"title\":\"Test Terms\"," +
                            "\"content\":\"Test content for terms that is long enough to meet validation requirements\"}";
        
        mockMvc.perform(post("/api/legal/docs")
                .contentType(APPLICATION_JSON)
                .content(jsonContent)
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testCreateDoc_WithoutRequiredField_ReturnsBadRequest() throws Exception {
        String jsonContent = "{\"type\":\"PRIVACY_POLICY\"," +
                            "\"version\":\"1.0\"," +
                            "\"content\":\"Test content\"}";  // Missing title
        
        mockMvc.perform(post("/api/legal/docs")
                .contentType(APPLICATION_JSON)
                .content(jsonContent)
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testCreateDoc_WithUser_IsForbidden() throws Exception {
        String jsonContent = "{\"type\":\"PRIVACY_POLICY\"," +
                            "\"version\":\"1.0\",\"title\":\"Test Privacy Policy\"," +
                            "\"content\":\"Test content for privacy policy that is long enough to meet validation requirements\"}";
        
        mockMvc.perform(post("/api/legal/docs")
                .contentType(APPLICATION_JSON)
                .content(jsonContent)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testCreateDoc_WithAdmin_IsForbidden() throws Exception {
        String jsonContent = "{\"type\":\"PRIVACY_POLICY\"," +
                            "\"version\":\"1.0\",\"title\":\"Test Privacy Policy\"," +
                            "\"content\":\"Test content for privacy policy that is long enough to meet validation requirements\"}";
        
        mockMvc.perform(post("/api/legal/docs")
                .contentType(APPLICATION_JSON)
                .content(jsonContent)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testCreateDoc_WithOrgOwner_IsForbidden() throws Exception {
        String jsonContent = "{\"type\":\"PRIVACY_POLICY\"," +
                            "\"version\":\"1.0\",\"title\":\"Test Privacy Policy\"," +
                            "\"content\":\"Test content for privacy policy that is long enough to meet validation requirements\"}";
        
        mockMvc.perform(post("/api/legal/docs")
                .contentType(APPLICATION_JSON)
                .content(jsonContent)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateDoc_WithoutAuthentication_Returns401() throws Exception {
        String jsonContent = "{\"type\":\"PRIVACY_POLICY\"," +
                            "\"version\":\"1.0\",\"title\":\"Test Privacy Policy\"," +
                            "\"content\":\"Test content for privacy policy that is long enough to meet validation requirements\"}";
        
        mockMvc.perform(post("/api/legal/docs")
                .contentType(APPLICATION_JSON)
                .content(jsonContent)
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL ACCEPT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testAccept_WithUser_ReturnsNotImplemented() throws Exception {
        AcceptLegalDocDto dto = new AcceptLegalDocDto();
        dto.setDocId(1L);
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/legal/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testAccept_WithSuperAdmin_ReturnsNotImplemented() throws Exception {
        AcceptLegalDocDto dto = new AcceptLegalDocDto();
        dto.setDocId(1L);
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/legal/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAccept_WithoutDocId_ReturnsBadRequest() throws Exception {
        AcceptLegalDocDto dto = new AcceptLegalDocDto();
        dto.setOrgId(testOrganization.getId().toString());
        // docId is missing
        
        mockMvc.perform(post("/api/legal/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAccept_WithoutAuthentication_Returns401() throws Exception {
        AcceptLegalDocDto dto = new AcceptLegalDocDto();
        dto.setDocId(1L);
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/legal/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL ACCEPTANCE STATUS TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testAcceptanceStatus_WithUser_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/legal/acceptance-status")
                .param("docType", "PRIVACY_POLICY")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testAcceptanceStatus_WithSuperAdmin_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/legal/acceptance-status")
                .param("docType", "PRIVACY_POLICY")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAcceptanceStatus_WithoutParams_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/legal/acceptance-status"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAcceptanceStatus_WithOnlyDocType_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/legal/acceptance-status")
                .param("docType", "PRIVACY_POLICY"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAcceptanceStatus_WithOnlyOrgId_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/legal/acceptance-status")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testAcceptanceStatus_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/legal/acceptance-status")
                .param("docType", "PRIVACY_POLICY")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isUnauthorized());
    }
}

