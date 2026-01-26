package com.mytegroup.api.controller.persons;

import com.mytegroup.api.dto.persons.CreatePersonDto;
import com.mytegroup.api.dto.persons.UpdatePersonDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.people.PersonType;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.service.persons.PersonsService;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PersonController.
 * Tests CRUD operations for persons/contacts, RBAC, request/response validation.
 */
class PersonControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Person testPerson;

    @Autowired
    private PersonsService personsService;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testPerson = setupPerson(testOrganization, "John", "Doe");
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testListPersons_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetPersonById_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/persons/1"))
                .andExpect(status().isUnauthorized());
    }

    // ========== RBAC TESTS - ALLOWED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListPersons_WithOrgAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/persons")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testListPersons_WithOrgOwner_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/persons")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testListPersons_WithAdmin_IsAllowed() throws Exception {
        mockMvc.perform(get("/api/persons")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk());
    }

    // ========== RBAC TESTS - DENIED ROLES ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testListPersons_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/persons")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ========== LIST ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListPersons_ReturnsPersonList() throws Exception {
        mockMvc.perform(get("/api/persons")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListPersons_WithPagination_ReturnsPaginatedResult() throws Exception {
        mockMvc.perform(get("/api/persons")
                .param("orgId", testOrganization.getId().toString())
                .param("page", "0")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10));
    }

    // ========== GET BY ID ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetPersonById_WithValidId_ReturnsPerson() throws Exception {
        mockMvc.perform(get("/api/persons/" + testPerson.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPerson.getId()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetPersonById_WithInvalidId_Returns404() throws Exception {
        mockMvc.perform(get("/api/persons/99999")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetPersonById_ResponseHasRequiredFields() throws Exception {
        mockMvc.perform(get("/api/persons/" + testPerson.getId())
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreatePerson_WithOrgAdmin_ReturnsCreated() throws Exception {
        CreatePersonDto dto = new CreatePersonDto(
                PersonType.INTERNAL_STAFF,  // personType (required)
                "New Person",  // displayName (required)
                "New",  // firstName
                "Person",  // lastName
                null,  // dateOfBirth
                null,  // emails (can be null, elements validated if provided)
                "newperson@test.com",  // primaryEmail
                null,  // phones (can be null, elements validated if provided)
                null,  // primaryPhone
                null,  // tagKeys
                null,  // skillKeys
                null,  // departmentKey
                null,  // orgLocationId
                null,  // reportsToPersonId
                null,  // ironworkerNumber
                null,  // unionLocal
                null,  // skillFreeText
                null,  // certifications
                null,  // rating
                null,  // notes
                null,  // companyId
                null,  // companyLocationId
                null   // title
        );
        mockMvc.perform(post("/api/persons")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testCreatePerson_WithUserRole_IsForbidden() throws Exception {
        CreatePersonDto dto = new CreatePersonDto(
                PersonType.INTERNAL_STAFF,  // personType
                "New Person",  // displayName
                "New",  // firstName
                "Person",  // lastName
                null,  // dateOfBirth
                null,  // emails
                "newperson@test.com",  // primaryEmail
                null,  // phones
                null,  // primaryPhone
                null,  // tagKeys
                null,  // skillKeys
                null,  // departmentKey
                null,  // orgLocationId
                null,  // reportsToPersonId
                null,  // ironworkerNumber
                null,  // unionLocal
                null,  // skillFreeText
                null,  // certifications
                null,  // rating
                null,  // notes
                null,  // companyId
                null,  // companyLocationId
                null   // title
        );
        mockMvc.perform(post("/api/persons")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== UPDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdatePerson_WithValidId_ReturnsOk() throws Exception {
        UpdatePersonDto dto = new UpdatePersonDto(
                PersonType.INTERNAL_STAFF,  // personType
                "Updated Person",  // displayName
                "Updated",  // firstName
                "Person",  // lastName
                null,  // dateOfBirth
                null,  // emails
                "updated@test.com",  // primaryEmail
                null,  // phones
                null,  // primaryPhone
                null,  // tagKeys
                null,  // skillKeys
                null,  // departmentKey
                null,  // orgLocationId
                null,  // reportsToPersonId
                null,  // ironworkerNumber
                null,  // unionLocal
                null,  // skillFreeText
                null,  // certifications
                null,  // rating
                null,  // notes
                null,  // companyId
                null,  // companyLocationId
                null   // title
        );
        mockMvc.perform(patch("/api/persons/" + testPerson.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testUpdatePerson_WithUserRole_IsForbidden() throws Exception {
        UpdatePersonDto dto = new UpdatePersonDto(
                PersonType.INTERNAL_STAFF,  // personType
                "Unauthorized Update",  // displayName
                null,  // firstName
                null,  // lastName
                null,  // dateOfBirth
                null,  // emails
                null,  // primaryEmail
                null,  // phones
                null,  // primaryPhone
                null,  // tagKeys
                null,  // skillKeys
                null,  // departmentKey
                null,  // orgLocationId
                null,  // reportsToPersonId
                null,  // ironworkerNumber
                null,  // unionLocal
                null,  // skillFreeText
                null,  // certifications
                null,  // rating
                null,  // notes
                null,  // companyId
                null,  // companyLocationId
                null   // title
        );
        mockMvc.perform(patch("/api/persons/" + testPerson.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== ARCHIVE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchivePerson_WithValidId_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/persons/" + testPerson.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPerson.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testArchivePerson_WithUserRole_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/persons/" + testPerson.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== CONTENT TYPE TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListPersons_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/api/persons")
                .param("orgId", testOrganization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    // ========== ADDITIONAL ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListPersons_WithoutOrgId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetPersonById_WithoutOrgId_ThrowsException() throws Exception {
        mockMvc.perform(get("/api/persons/" + testPerson.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdatePerson_WithoutOrgId_ThrowsException() throws Exception {
        UpdatePersonDto dto = new UpdatePersonDto(
            PersonType.INTERNAL_STAFF, // personType
            "Updated", // displayName
            null, // firstName
            null, // lastName
            null, // dateOfBirth
            null, // emails
            null, // primaryEmail
            null, // phones
            null, // primaryPhone
            null, // tagKeys
            null, // skillKeys
            null, // departmentKey
            null, // orgLocationId
            null, // reportsToPersonId
            null, // ironworkerNumber
            null, // unionLocal
            null, // skillFreeText
            null, // certifications
            null, // rating
            null, // notes
            null, // companyId
            null, // companyLocationId
            null  // title
        );
        mockMvc.perform(patch("/api/persons/" + testPerson.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUnarchivePerson_WithValidId_ReturnsOk() throws Exception {
        // First archive the person so we can unarchive it
        personsService.archive(testPerson.getId(), testOrganization.getId().toString());
        
        mockMvc.perform(post("/api/persons/" + testPerson.getId() + "/unarchive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPerson.getId()));
    }
}

