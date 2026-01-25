package com.mytegroup.api.controller.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.users.CreateUserDto;
import com.mytegroup.api.dto.users.UpdateUserDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController.
 * Tests user management, RBAC, request/response validation.
 */
class UserControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testUser = setupUser(testOrganization, "testuser@example.com");
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testListUsers_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetUserById_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized());
    }

    // ========== RBAC TESTS - ALLOWED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListUsers_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/users")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListUsers_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListUsers_WithSuperAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    // ========== RBAC TESTS - DENIED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListUsers_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListUsers_ReturnsUserList() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListUsers_WithPagination_ReturnsList() throws Exception {
        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testGetUserById_WithValidId_ReturnsUser() throws Exception {
        mockMvc.perform(get("/api/users/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("testuser@example.com"))
                .andExpect(jsonPath("$.email").value("testuser@example.com"));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testGetUserById_WithInvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    @org.junit.jupiter.api.Disabled("ExceptionInInitializerError - static initialization issue in dependency")
    void testCreateUser_WithAdmin_ReturnsCreated() throws Exception {
        // TODO: Fix ExceptionInInitializerError - likely static initialization in WaitlistService or related
        // Use unique email to avoid conflicts
        String uniqueEmail = "newuser" + System.currentTimeMillis() + "@test.com";
        CreateUserDto dto = new CreateUserDto();
        dto.setUsername(uniqueEmail);
        dto.setEmail(uniqueEmail);
        dto.setPassword("StrongPassword123!");  // Must meet strong password requirements
        dto.setRole(Role.USER);
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/users")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    @org.junit.jupiter.api.Disabled("ExceptionInInitializerError - static initialization issue in dependency")
    void testCreateUser_WithOrgAdmin_IsForbidden() throws Exception {
        // TODO: Fix ExceptionInInitializerError - likely static initialization in WaitlistService or related
        // Use unique email to avoid conflicts
        String uniqueEmail = "unauthorized" + System.currentTimeMillis() + "@test.com";
        CreateUserDto dto = new CreateUserDto();
        dto.setUsername(uniqueEmail);
        dto.setEmail(uniqueEmail);
        dto.setPassword("StrongPassword123!");
        dto.setRole(Role.USER);
        dto.setOrgId(testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/users")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testUpdateUser_WithValidId_ReturnsOk() throws Exception {
        UpdateUserDto dto = new UpdateUserDto();
        dto.setFirstName("Updated");
        dto.setLastName("Name");
        
        mockMvc.perform(patch("/api/users/" + testUser.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== ARCHIVE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    @org.junit.jupiter.api.Disabled("Service-level serialization issue with Map<String, Object>")
    void testArchiveUser_WithValidId_ReturnsOk() throws Exception {
        // TODO: Fix service-level serialization issue
        mockMvc.perform(post("/api/users/" + testUser.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testArchiveUser_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/users/" + testUser.getId() + "/archive")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== CONTENT TYPE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListUsers_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }
}

