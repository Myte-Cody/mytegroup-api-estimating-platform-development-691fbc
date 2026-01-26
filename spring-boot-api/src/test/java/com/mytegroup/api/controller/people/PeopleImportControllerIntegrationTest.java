package com.mytegroup.api.controller.people;

import com.mytegroup.api.dto.people.PeopleImportConfirmDto;
import com.mytegroup.api.dto.people.PeopleImportV1ConfirmDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.people.PersonType;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PeopleImportController.
 * Tests people import operations, RBAC, request/response validation.
 */
class PeopleImportControllerIntegrationTest extends BaseControllerTest {

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
    void testPreview_WithOrgAdmin_ReturnsNotImplemented() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        // This endpoint throws UnsupportedOperationException
        mockMvc.perform(multipart("/api/people/import/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testPreview_WithUserRole_IsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== PREVIEW ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreview_WithoutOrgId_ReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/preview")
                .file(file)
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("orgId is required"));
    }

    // ========== CONFIRM ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testConfirm_WithValidData_ReturnsOk() throws Exception {
        // Create a valid row (records require all fields)
        com.mytegroup.api.dto.people.PeopleImportConfirmRowDto row = 
            new com.mytegroup.api.dto.people.PeopleImportConfirmRowDto(
                1, // row
                null, // personType
                "Test User", // name
                "test@example.com", // email
                null, // phone
                null, // company
                null, // ironworkerNumber
                null, // unionLocal
                null, // dateOfBirth
                null, // skills
                null, // certifications
                null, // notes
                null, // inviteRole
                "create", // action
                null, // personId
                null  // contactId
            );
        
        PeopleImportConfirmDto dto = new PeopleImportConfirmDto();
        dto.setConfirmedRows(new java.util.ArrayList<>(java.util.List.of(row)));
        
        // Note: This may fail if service has validation issues, but tests endpoint
        mockMvc.perform(post("/api/people/import/confirm")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").exists());
    }

    // ========== V1 ENDPOINT TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewV1_WithOrgAdmin_ReturnsNotImplemented() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        // This endpoint throws UnsupportedOperationException
        mockMvc.perform(multipart("/api/people/import/v1/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testConfirmV1_WithOrgAdmin_ReturnsNotImplemented() throws Exception {
        // Create a valid row (records require all fields - 27 total)
        com.mytegroup.api.dto.people.PeopleImportV1ConfirmRowDto row = 
            new com.mytegroup.api.dto.people.PeopleImportV1ConfirmRowDto(
                1, // row
                PersonType.EXTERNAL_PERSON, // personType
                "Test User", // displayName
                null, // emails
                null, // primaryEmail
                null, // phones
                null, // primaryPhone
                null, // tagKeys
                null, // skillKeys
                null, // departmentKey
                null, // title
                null, // orgLocationName
                null, // reportsToDisplayName
                null, // companyExternalId
                null, // companyName
                null, // companyLocationExternalId
                null, // companyLocationName
                null, // ironworkerNumber
                null, // unionLocal
                null, // certifications
                null, // rating
                null, // notes
                null, // inviteRole
                "create", // action
                null  // personId
            );
        
        PeopleImportV1ConfirmDto dto = new PeopleImportV1ConfirmDto();
        dto.setConfirmedRows(new java.util.ArrayList<>(java.util.List.of(row)));
        
        // This endpoint throws UnsupportedOperationException
        mockMvc.perform(post("/api/people/import/v1/confirm")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== ADDITIONAL PREVIEW TESTS ==========

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testPreview_WithSuperAdmin_ReturnsNotImplemented() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_OWNER)
    void testPreview_WithOrgOwner_ReturnsNotImplemented() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void testPreview_WithAdmin_ReturnsNotImplemented() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testPreview_WithPlatformAdmin_ReturnsNotImplemented() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testPreview_WithoutAuthentication_Returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL CONFIRM TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testConfirm_WithUserRole_IsForbidden() throws Exception {
        com.mytegroup.api.dto.people.PeopleImportConfirmRowDto row = 
            new com.mytegroup.api.dto.people.PeopleImportConfirmRowDto(
                1, null, "Test User", "test@example.com", null, null, null, null, 
                null, null, null, null, null, "create", null, null
            );
        
        PeopleImportConfirmDto dto = new PeopleImportConfirmDto();
        dto.setConfirmedRows(new java.util.ArrayList<>(java.util.List.of(row)));
        
        mockMvc.perform(post("/api/people/import/confirm")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testConfirm_WithoutOrgId_ReturnsBadRequest() throws Exception {
        com.mytegroup.api.dto.people.PeopleImportConfirmRowDto row = 
            new com.mytegroup.api.dto.people.PeopleImportConfirmRowDto(
                1, null, "Test User", "test@example.com", null, null, null, null, 
                null, null, null, null, null, "create", null, null
            );
        
        PeopleImportConfirmDto dto = new PeopleImportConfirmDto();
        dto.setConfirmedRows(new java.util.ArrayList<>(java.util.List.of(row)));
        
        mockMvc.perform(post("/api/people/import/confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("orgId is required"));
    }

    @Test
    void testConfirm_WithoutAuthentication_Returns401() throws Exception {
        com.mytegroup.api.dto.people.PeopleImportConfirmRowDto row = 
            new com.mytegroup.api.dto.people.PeopleImportConfirmRowDto(
                1, null, "Test User", "test@example.com", null, null, null, null, 
                null, null, null, null, null, "create", null, null
            );
        
        PeopleImportConfirmDto dto = new PeopleImportConfirmDto();
        dto.setConfirmedRows(new java.util.ArrayList<>(java.util.List.of(row)));
        
        mockMvc.perform(post("/api/people/import/confirm")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL V1 PREVIEW TESTS ==========

    @Test
    @WithMockUser(roles = ROLE_USER)
    void testPreviewV1_WithUserRole_IsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/v1/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testPreviewV1_WithoutOrgId_ReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/v1/preview")
                .file(file)
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("orgId is required"));
    }

    @Test
    void testPreviewV1_WithoutAuthentication_Returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "name,email\nJohn Doe,john@example.com".getBytes()
        );
        
        mockMvc.perform(multipart("/api/people/import/v1/preview")
                .file(file)
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ========== ADDITIONAL V1 CONFIRM TESTS (Skipped due to complex DTO) ==========
    // Note: V1 tests intentionally minimal due to DTO constructor complexity
}

