package com.mytegroup.api.service.companies;

import com.mytegroup.api.BaseIntegrationTest;
import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.service.common.ActorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Example service integration test demonstrating the test infrastructure.
 * Tests CompaniesService with real database using Testcontainers.
 */
class CompaniesServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CompaniesService companiesService;

    private Organization testOrg;
    private User testUser;
    private ActorContext adminActor;

    @BeforeEach
    void setUpTestData() {
        // Setup test data using helper methods from BaseIntegrationTest
        testOrg = setupOrganization("Test Org");
        testUser = setupUser(testOrg, "admin@test.com");
        adminActor = new ActorContext(testUser.getId().toString(), testOrg.getId().toString(), Role.ADMIN);
    }

    @Test
    void shouldCreateCompany() {
        // Given
        Company company = new Company();
        company.setName("Test Company");
        company.setNormalizedName("testcompany");

        // When
        Company created = companiesService.create(company, adminActor, testOrg.getId().toString());

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Company");
        assertThat(created.getOrganization().getId()).isEqualTo(testOrg.getId());
    }

    @Test
    void shouldNotCreateDuplicateCompany() {
        // Given
        Company company1 = new Company();
        company1.setName("Test Company");
        company1.setNormalizedName("testcompany");
        companiesService.create(company1, adminActor, testOrg.getId().toString());

        Company company2 = new Company();
        company2.setName("Test Company");
        company2.setNormalizedName("testcompany");

        // When/Then
        assertThatThrownBy(() -> companiesService.create(company2, adminActor, testOrg.getId().toString()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Company already exists");
    }

    @Test
    void shouldNotAllowUnauthorizedUser() {
        // Given
        ActorContext viewerActor = new ActorContext(testUser.getId().toString(), testOrg.getId().toString(), Role.VIEWER);
        Company company = new Company();
        company.setName("Test Company");
        company.setNormalizedName("testcompany");

        // When/Then
        assertThatThrownBy(() -> companiesService.create(company, viewerActor, testOrg.getId().toString()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldGetCompanyById() {
        // Given
        Company company = new Company();
        company.setName("Test Company");
        company.setNormalizedName("testcompany");
        Company created = companiesService.create(company, adminActor, testOrg.getId().toString());

        // When
        Company found = companiesService.getById(created.getId(), adminActor, testOrg.getId().toString(), false);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Test Company");
    }
}

