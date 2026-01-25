package com.mytegroup.api.controller.health;

import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for HealthController.
 * Tests health check endpoints and system status.
 */
class HealthControllerIntegrationTest extends BaseControllerTest {

    // ========== HEALTH CHECK ENDPOINT TESTS ==========

    @Test
    void testHealth_WithoutAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testHealth_WithAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealth_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    @Test
    void testHealth_ResponseHasStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    // ========== READINESS ENDPOINT TESTS ==========
    // Note: HealthController uses /ready, not /readiness

    @Test
    void testReadiness_WithoutAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health/ready"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testReadiness_WithAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health/ready"))
                .andExpect(status().isOk());
    }

    // ========== LIVENESS ENDPOINT TESTS ==========
    // Note: HealthController uses /live, not /liveness

    @Test
    void testLiveness_WithoutAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health/live"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testLiveness_WithAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health/live"))
                .andExpect(status().isOk());
    }

    // ========== STATUS ENDPOINT TESTS ==========
    // Note: HealthController does not have a /status endpoint - using main /health endpoint

    @Test
    void testStatus_WithoutAuthentication_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testStatus_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }
}

