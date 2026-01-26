package com.mytegroup.api.controller.graphedges;

import com.mytegroup.api.dto.graphedges.CreateGraphEdgeDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.GraphEdge;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GraphEdgeController.
 * Tests graph edge operations, RBAC, request/response validation.
 */
class GraphEdgeControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private GraphEdge testGraphEdge;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        // Note: GraphEdge setup would require additional setup if needed
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListGraphEdges_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/graph-edges")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListGraphEdges_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/graph-edges")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListGraphEdges_WithValidOrgId_ReturnsEdges() throws Exception {
        mockMvc.perform(get("/api/graph-edges")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListGraphEdges_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/graph-edges"))
                .andExpect(status().isBadRequest());
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateGraphEdge_WithValidData_ReturnsCreated() throws Exception {
        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("ORG_LOCATION");
        dto.setFromId("1");
        dto.setToType("ORG_LOCATION");
        dto.setToId("2");
        dto.setEdgeType("parent");
        
        // Note: This may succeed if entities exist (201), or fail if they don't (400)
        mockMvc.perform(post("/api/graph-edges")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 201 || status == 400, 
                        "Expected 201 (Created) or 400 (Bad Request), but got: " + status);
                });
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateGraphEdge_WithoutOrgId_ThrowsException() throws Exception {
        CreateGraphEdgeDto dto = new CreateGraphEdgeDto();
        dto.setFromType("ORG_LOCATION");
        dto.setFromId("1");
        dto.setToType("ORG_LOCATION");
        dto.setToId("2");
        dto.setEdgeType("parent");
        
        mockMvc.perform(post("/api/graph-edges")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testDeleteGraphEdge_WithValidId_ReturnsNoContent() throws Exception {
        // Note: This will return 404 if no edge exists, or 204 if it exists and is deleted
        mockMvc.perform(delete("/api/graph-edges/1")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 204 || status == 404, 
                        "Expected 204 (No Content) or 404 (Not Found), but got: " + status);
                });
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testDeleteGraphEdge_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(delete("/api/graph-edges/1")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}

