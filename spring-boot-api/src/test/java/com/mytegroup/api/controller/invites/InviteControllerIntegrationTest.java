package com.mytegroup.api.controller.invites;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.invites.CreateInviteDto;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.core.InviteStatus;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for InviteController.
 * Tests invite management, RBAC, request/response validation.
 */
class InviteControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Person testPerson;
    private Invite testInvite;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testPerson = setupPerson(testOrganization, "John", "Doe");
        testInvite = setupInvite(testOrganization, testPerson);
    }

    @Test
    void testListInvites_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/invites"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListInvites_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListInvites_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListInvites_ReturnsInviteList() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()); // Returns array directly, not wrapped in data
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateInvite_WithOrgAdmin_ReturnsCreated() throws Exception {
        // Create a new person with unique email to avoid conflicts
        // Use timestamp and random to ensure uniqueness in both name and email
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 100000);
        String uniqueFirstName = "Invite" + timestamp + random;
        String uniqueLastName = "Person" + random;
        String uniqueEmail = "invite" + timestamp + random + "@example.com";
        
        // Create person directly with unique email to avoid any conflicts
        // Don't use setupPerson as it creates a default email that might conflict
        Person newPerson = new Person();
        newPerson.setOrganization(testOrganization);
        newPerson.setFirstName(uniqueFirstName);
        newPerson.setLastName(uniqueLastName);
        newPerson.setDisplayName(uniqueFirstName + " " + uniqueLastName);
        newPerson.setPrimaryEmail(uniqueEmail);
        newPerson.setPersonType(com.mytegroup.api.entity.enums.people.PersonType.INTERNAL_STAFF);
        newPerson = personRepository.save(newPerson);
        personRepository.flush(); // Ensure it's persisted
        
        // Refresh from database to get latest state (including user relationship)
        final Person finalPerson = personRepository.findById(newPerson.getId()).orElseThrow();
        
        // Verify person doesn't have a user linked
        assertNull(finalPerson.getUser(), "Person should not have a user linked");
        
        // Ensure no user exists with this email (the service checks this)
        com.mytegroup.api.entity.core.User existingUser = userRepository.findByEmail(uniqueEmail.toLowerCase().trim()).orElse(null);
        if (existingUser != null) {
            userRepository.delete(existingUser);
            userRepository.flush();
        }
        
        // Clean up any existing pending invites for this email
        Optional<Invite> existingInvite = inviteRepository.findPendingActiveInvite(
                testOrganization.getId(), uniqueEmail.toLowerCase().trim(), 
                java.time.LocalDateTime.now());
        if (existingInvite.isPresent()) {
            Invite invite = existingInvite.get();
            invite.setStatus(InviteStatus.EXPIRED);
            inviteRepository.save(invite);
            inviteRepository.flush();
        }
        
        CreateInviteDto dto = new CreateInviteDto(
                finalPerson.getId().toString(),  // personId (required)
                Role.USER,  // role (required)
                72  // expiresInHours (optional, default 72)
        );
        // Perform the request and capture response for debugging
        mockMvc.perform(post("/api/invites")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 201) {
                        String content = result.getResponse().getContentAsString();
                        System.err.println("=== INVITE TEST FAILURE ===");
                        System.err.println("Status: " + status);
                        System.err.println("Response: " + content);
                        System.err.println("Person ID: " + finalPerson.getId());
                        System.err.println("Person Email: " + finalPerson.getPrimaryEmail());
                        System.err.println("Person has User: " + (finalPerson.getUser() != null));
                        System.err.println("===========================");
                    }
                })
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.personId").value(finalPerson.getId()))
                .andExpect(jsonPath("$.role").value(Role.USER.getValue()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testResendInvite_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/invites/{id}/resend", testInvite.getId())
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCancelInvite_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/invites/{id}/cancel", testInvite.getId())
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListInvites_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    // ========== ACCEPT INVITE ENDPOINT TESTS ==========

    @Test
    void testAcceptInvite_WithoutAuthentication_IsAllowed() throws Exception {
        com.mytegroup.api.dto.invites.AcceptInviteDto dto = 
            new com.mytegroup.api.dto.invites.AcceptInviteDto(
                "test-token",
                "testuser",
                "ValidPassword123!@"
            );
        
        // This may fail if token is invalid, but tests the endpoint
        mockMvc.perform(post("/api/invites/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isNotFound()); // Service throws ResourceNotFoundException when invite not found
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListInvites_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/invites"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testResendInvite_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(post("/api/invites/" + testInvite.getId() + "/resend")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCancelInvite_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(post("/api/invites/" + testInvite.getId() + "/cancel")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL LIST TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListInvites_WithStatusFilter_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString())
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListInvites_WithAcceptedStatus_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString())
                .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListInvites_WithExpiredStatus_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString())
                .param("status", "EXPIRED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ========== ADDITIONAL CREATE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateInvite_WithoutOrgId_ThrowsException() throws Exception {
        CreateInviteDto dto = new CreateInviteDto(
                testPerson.getId().toString(), Role.USER, 72
        );
        mockMvc.perform(post("/api/invites")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateInvite_WithoutPersonId_ReturnsBadRequest() throws Exception {
        CreateInviteDto dto = new CreateInviteDto(
                null, Role.USER, 72 // Missing personId
        );
        mockMvc.perform(post("/api/invites")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL RESEND TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testResendInvite_WithInvalidId_ReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/invites/99999/resend")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ========== ADDITIONAL CANCEL TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCancelInvite_WithInvalidId_ReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/invites/99999/cancel")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ========== ADDITIONAL ACCEPT TESTS ==========

    @Test
    void testAcceptInvite_WithoutToken_ReturnsBadRequest() throws Exception {
        com.mytegroup.api.dto.invites.AcceptInviteDto dto = 
            new com.mytegroup.api.dto.invites.AcceptInviteDto(
                null, // Missing token
                "testuser",
                "ValidPassword123!@"
            );
        
        mockMvc.perform(post("/api/invites/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAcceptInvite_WithoutUsername_ReturnsBadRequest() throws Exception {
        com.mytegroup.api.dto.invites.AcceptInviteDto dto = 
            new com.mytegroup.api.dto.invites.AcceptInviteDto(
                "test-token",
                null, // Missing username
                "ValidPassword123!@"
            );
        
        mockMvc.perform(post("/api/invites/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAcceptInvite_WithoutPassword_ReturnsBadRequest() throws Exception {
        com.mytegroup.api.dto.invites.AcceptInviteDto dto = 
            new com.mytegroup.api.dto.invites.AcceptInviteDto(
                "test-token",
                "testuser",
                null // Missing password
            );
        
        mockMvc.perform(post("/api/invites/accept")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== ADDITIONAL RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testListInvites_WithOrgOwner_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_SUPER_ADMIN)
    void testListInvites_WithSuperAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListInvites_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/invites")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

