package com.mytegroup.api.controller.seats;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.seats.SeatsService;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SeatController.
 * Tests seat management operations, RBAC, request/response validation.
 */
class SeatControllerIntegrationTest extends BaseControllerTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SeatsService seatsService;

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testUser = setupUser(testOrganization, "test@example.com");
        testUser.setRole(com.mytegroup.api.common.enums.Role.USER);
        testUser.setRoles(new java.util.ArrayList<>(java.util.List.of(com.mytegroup.api.common.enums.Role.USER)));
        testUser = userRepository.save(testUser);
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListSeats_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListSeats_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListSeats_WithValidOrgId_ReturnsSeats() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListSeats_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/seats"))
                .andExpect(status().isBadRequest());
    }

    // ========== SUMMARY ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetSeatSummary_WithValidOrgId_ReturnsSummary() throws Exception {
        mockMvc.perform(get("/api/seats/summary")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgId").exists())
                .andExpect(jsonPath("$.total").exists());
    }

    // ========== ENSURE SEATS ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testEnsureSeats_WithValidParams_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/seats/ensure")
                .param("orgId", testOrganization.getId().toString())
                .param("totalSeats", "10")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // ========== ALLOCATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAllocateSeat_WithValidParams_ReturnsSeat() throws Exception {
        // First ensure seats exist
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        
        mockMvc.perform(post("/api/seats/allocate")
                .param("orgId", testOrganization.getId().toString())
                .param("userId", testUser.getId().toString())
                .param("role", "user")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    // ========== ADDITIONAL EDGE CASE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListSeats_WithStatusFilter_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString())
                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListSeats_WithVacantStatus_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString())
                .param("status", "VACANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetSeatSummary_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/seats/summary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testEnsureSeats_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(post("/api/seats/ensure")
                .param("totalSeats", "10")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testEnsureSeats_WithDefaultTotalSeats_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/seats/ensure")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAllocateSeat_WithoutUserId_ThrowsException() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        
        mockMvc.perform(post("/api/seats/allocate")
                .param("orgId", testOrganization.getId().toString())
                .param("role", "user")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAllocateSeat_WithProjectId_ReturnsSeat() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        com.mytegroup.api.entity.organization.Office office = setupOffice(testOrganization);
        com.mytegroup.api.entity.projects.Project project = setupProject(testOrganization, office);
        
        mockMvc.perform(post("/api/seats/allocate")
                .param("orgId", testOrganization.getId().toString())
                .param("userId", testUser.getId().toString())
                .param("role", "user")
                .param("projectId", project.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testReleaseSeat_WithValidParams_ReturnsOk() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        seatsService.allocateSeat(testOrganization.getId().toString(), testUser.getId(), "user", null);
        
        mockMvc.perform(post("/api/seats/release")
                .param("orgId", testOrganization.getId().toString())
                .param("userId", testUser.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testReleaseSeat_WithoutSeat_ReturnsNoSeatFound() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        
        // When no seat is allocated, release returns 404
        mockMvc.perform(post("/api/seats/release")
                .param("orgId", testOrganization.getId().toString())
                .param("userId", testUser.getId().toString())
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAssignProject_WithValidParams_ReturnsSeat() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        com.mytegroup.api.entity.organization.Office office = setupOffice(testOrganization);
        com.mytegroup.api.entity.projects.Project project = setupProject(testOrganization, office);
        com.mytegroup.api.entity.projects.Seat seat = seatsService.allocateSeat(
            testOrganization.getId().toString(), testUser.getId(), "user", null);
        
        mockMvc.perform(post("/api/seats/" + seat.getId() + "/assign-project")
                .param("orgId", testOrganization.getId().toString())
                .param("projectId", project.getId().toString())
                .param("role", "user")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seat.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testClearProject_WithValidParams_ReturnsSeat() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        com.mytegroup.api.entity.organization.Office office = setupOffice(testOrganization);
        com.mytegroup.api.entity.projects.Project project = setupProject(testOrganization, office);
        com.mytegroup.api.entity.projects.Seat seat = seatsService.allocateSeat(
            testOrganization.getId().toString(), testUser.getId(), "user", project.getId());
        
        mockMvc.perform(post("/api/seats/" + seat.getId() + "/clear-project")
                .param("orgId", testOrganization.getId().toString())
                .param("projectId", project.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seat.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testAssignProject_WithoutOrgId_ThrowsException() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        com.mytegroup.api.entity.projects.Seat seat = seatsService.allocateSeat(
            testOrganization.getId().toString(), testUser.getId(), "user", null);
        
        mockMvc.perform(post("/api/seats/" + seat.getId() + "/assign-project")
                .param("projectId", "1")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testClearProject_WithoutOrgId_ThrowsException() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        com.mytegroup.api.entity.projects.Seat seat = seatsService.allocateSeat(
            testOrganization.getId().toString(), testUser.getId(), "user", null);
        
        mockMvc.perform(post("/api/seats/" + seat.getId() + "/clear-project")
                .param("projectId", "1")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListSeats_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListSeats_WithSuperAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testListSeats_WithOrgOwner_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_PLATFORM_ADMIN)
    void testListSeats_WithPlatformAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetSeatSummary_ResponseContainsAllFields() throws Exception {
        seatsService.ensureOrgSeats(testOrganization.getId().toString(), 10);
        
        mockMvc.perform(get("/api/seats/summary")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgId").exists())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.active").exists())
                .andExpect(jsonPath("$.vacant").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListSeats_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    @Test
    void testListSeats_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/seats")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isUnauthorized());
    }
}

