package com.mytegroup.api.controller.companies;

import com.mytegroup.api.BaseControllerTest;
import com.mytegroup.api.dto.companies.CreateCompanyDto;
import com.mytegroup.api.dto.companies.UpdateCompanyDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.service.companies.CompaniesService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Example controller test demonstrating the controller test infrastructure.
 * Tests CompanyController with mocked services.
 */
@WebMvcTest(CompanyController.class)
@WithMockUser(roles = "ADMIN")
class CompanyControllerTest extends BaseControllerTest {

    @MockBean
    private CompaniesService companiesService;

    @Test
    void shouldCreateCompany() throws Exception {
        // Given
        CreateCompanyDto dto = new CreateCompanyDto(
                "Test Company",
                "EXT-001",
                "https://test.com",
                "test@test.com",
                "+1234567890",
                List.of("type1"),
                List.of("tag1"),
                4.5,
                "Test notes"
        );

        Company savedCompany = new Company();
        savedCompany.setId(1L);
        savedCompany.setName("Test Company");
        savedCompany.setNormalizedName("testcompany");
        Organization org = new Organization();
        org.setId(1L);
        savedCompany.setOrganization(org);

        when(companiesService.create(any(Company.class), any(), anyString()))
                .thenReturn(savedCompany);

        // When/Then
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Company"));
    }

    @Test
    void shouldGetCompanyById() throws Exception {
        // Given
        Company company = new Company();
        company.setId(1L);
        company.setName("Test Company");
        company.setNormalizedName("testcompany");
        Organization org = new Organization();
        org.setId(1L);
        company.setOrganization(org);

        when(companiesService.getById(eq(1L), any(), anyString(), eq(false)))
                .thenReturn(company);

        // When/Then
        mockMvc.perform(get("/api/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Company"));
    }

    @Test
    void shouldListCompanies() throws Exception {
        // Given
        Company company = new Company();
        company.setId(1L);
        company.setName("Test Company");
        company.setNormalizedName("testcompany");
        Organization org = new Organization();
        org.setId(1L);
        company.setOrganization(org);

        Page<Company> page = new PageImpl<>(List.of(company), PageRequest.of(0, 25), 1);

        when(companiesService.list(any(), anyString(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldUpdateCompany() throws Exception {
        // Given
        UpdateCompanyDto dto = new UpdateCompanyDto(
                "Updated Company",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Company updatedCompany = new Company();
        updatedCompany.setId(1L);
        updatedCompany.setName("Updated Company");
        updatedCompany.setNormalizedName("updatedcompany");
        Organization org = new Organization();
        org.setId(1L);
        updatedCompany.setOrganization(org);

        when(companiesService.update(eq(1L), any(Company.class), any(), anyString()))
                .thenReturn(updatedCompany);

        // When/Then
        mockMvc.perform(patch("/api/companies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Updated Company"));
    }
}

