package com.mytegroup.api.controller.people;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.Contact;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ContactController.
 * Tests contact CRUD operations, RBAC, request/response validation.
 */
class ContactControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Contact testContact;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testContact = setupContact(testOrganization, "John", "Doe");
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testListContacts_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isUnauthorized());
    }

    // ========== RBAC TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListContacts_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/contacts")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListContacts_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/contacts")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListContacts_ReturnsContactList() throws Exception {
        mockMvc.perform(get("/api/contacts")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetContactById_WithValidId_ReturnsContact() throws Exception {
        mockMvc.perform(get("/api/contacts/" + testContact.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetContactById_WithInvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/contacts/99999")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    // ========== CREATE ENDPOINT TESTS ==========
    // NOTE: ContactController does not have POST/PATCH endpoints - these are disabled

    @Test
    @Disabled("ContactController does not have a create endpoint")
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateContact_WithOrgAdmin_ReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/contacts")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @Disabled("ContactController does not have an update endpoint")
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateContact_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/contacts/" + testContact.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== ARCHIVE ENDPOINT TESTS ==========

    @Test
    @Disabled("ContactController does not have an archive endpoint")
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveContact_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/contacts/" + testContact.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk());
    }
}

