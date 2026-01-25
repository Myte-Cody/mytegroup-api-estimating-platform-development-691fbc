package com.mytegroup.api.controller.ingestion;

import com.mytegroup.api.dto.ingestion.*;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for IngestionContactsController.
 * Tests contact ingestion operations, RBAC, request/response validation.
 */
class IngestionContactsControllerIntegrationTest extends BaseControllerTest {

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
    void testSuggestMapping_WithOrgAdmin_IsAllowed() throws Exception {
        IngestionContactsSuggestMappingDto dto = new IngestionContactsSuggestMappingDto();
        dto.setProfile("default");
        dto.setHeaders(List.of("name", "email", "phone"));
        dto.setSampleRows(List.of(
            Map.of("name", "John Doe", "email", "john@example.com", "phone", "+15145551234")
        ));
        
        mockMvc.perform(post("/api/ingestion/contacts/v1/suggest-mapping")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testSuggestMapping_WithUserRole_IsForbidden() throws Exception {
        IngestionContactsSuggestMappingDto dto = new IngestionContactsSuggestMappingDto();
        dto.setProfile("default");
        dto.setHeaders(List.of("name", "email"));
        dto.setSampleRows(List.of());
        
        mockMvc.perform(post("/api/ingestion/contacts/v1/suggest-mapping")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== SUGGEST MAPPING ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSuggestMapping_WithoutOrgId_ReturnsBadRequest() throws Exception {
        IngestionContactsSuggestMappingDto dto = new IngestionContactsSuggestMappingDto();
        dto.setProfile("default");
        dto.setHeaders(List.of("name", "email"));
        dto.setSampleRows(List.of());
        
        mockMvc.perform(post("/api/ingestion/contacts/v1/suggest-mapping")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("orgId is required"));
    }

    // ========== PARSE ROW ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testParseRow_WithValidData_ReturnsParsed() throws Exception {
        IngestionContactsParseRowDto dto = new IngestionContactsParseRowDto();
        dto.setProfile("default");
        dto.setRow(Map.of("name", "John Doe", "email", "john@example.com"));
        dto.setMapping(Map.of("name", "name", "email", "email"));
        
        mockMvc.perform(post("/api/ingestion/contacts/v1/parse-row")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    // ========== ENRICH ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testEnrich_WithValidData_ReturnsServiceUnavailable() throws Exception {
        IngestionContactsEnrichDto dto = new IngestionContactsEnrichDto();
        dto.setProfile("default");
        dto.setContact(Map.of("email", "john@example.com", "name", "John Doe"));
        
        // This endpoint throws ServiceUnavailableException (not yet implemented)
        mockMvc.perform(post("/api/ingestion/contacts/v1/enrich")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }
}

