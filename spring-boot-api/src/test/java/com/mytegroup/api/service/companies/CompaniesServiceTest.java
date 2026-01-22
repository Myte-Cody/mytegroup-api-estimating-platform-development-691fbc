package com.mytegroup.api.service.companies;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.companies.CompanyRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CompaniesService.
 * Tests business logic in isolation with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class CompaniesServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private ServiceValidationHelper validationHelper;

    @InjectMocks
    private CompaniesService companiesService;

    private Organization testOrg;
    private Company testCompany;
    private ActorContext adminActor;

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId(1L);
        testOrg.setName("Test Org");

        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setName("Test Company");
        testCompany.setNormalizedName("testcompany");
        testCompany.setOrganization(testOrg);

        adminActor = new ActorContext("1", "1", Role.ADMIN);
    }

    @Test
    void shouldCreateCompany() {
        // Given
        Company company = new Company();
        company.setName("New Company");
        company.setNormalizedName("newcompany");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(validationHelper.normalizeName("New Company")).thenReturn("newcompany");
        when(companyRepository.findByOrgIdAndNormalizedName(1L, "newcompany"))
                .thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);

        // When
        Company created = companiesService.create(company, adminActor, "1");

        // Then
        assertThat(created).isNotNull();
        verify(authHelper).ensureRole(adminActor, Role.ORG_OWNER, Role.ORG_ADMIN, Role.ADMIN, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN);
        verify(authHelper).ensureOrgScope("1", adminActor);
        verify(companyRepository).save(any(Company.class));
        verify(auditLogService).log(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldThrowConflictExceptionForDuplicateCompany() {
        // Given
        Company company = new Company();
        company.setName("Test Company");
        company.setNormalizedName("testcompany");

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(validationHelper.normalizeName("Test Company")).thenReturn("testcompany");
        when(companyRepository.findByOrgIdAndNormalizedName(1L, "testcompany"))
                .thenReturn(Optional.of(testCompany));

        // When/Then
        assertThatThrownBy(() -> companiesService.create(company, adminActor, "1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Company already exists");

        verify(companyRepository, never()).save(any());
    }

    @Test
    void shouldThrowForbiddenExceptionForUnauthorizedUser() {
        // Given
        ActorContext viewerActor = new ActorContext("1", "1", Role.VIEWER);
        Company company = new Company();
        company.setName("Test Company");

        doThrow(new ForbiddenException("Insufficient role"))
                .when(authHelper).ensureRole(viewerActor, Role.ORG_OWNER, Role.ORG_ADMIN, Role.ADMIN, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN);

        // When/Then
        assertThatThrownBy(() -> companiesService.create(company, viewerActor, "1"))
                .isInstanceOf(ForbiddenException.class);

        verify(companyRepository, never()).save(any());
    }

    @Test
    void shouldGetCompanyById() {
        // Given
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));

        // When
        Company found = companiesService.getById(1L, adminActor, "1", false);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(1L);
        verify(authHelper).ensureRole(adminActor, Role.ORG_OWNER, Role.ORG_ADMIN, Role.ADMIN, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN);
        verify(authHelper).ensureOrgScope("1", adminActor);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> companiesService.getById(999L, adminActor, "1", false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    void shouldListCompanies() {
        // Given
        Page<Company> page = new PageImpl<>(List.of(testCompany), PageRequest.of(0, 25), 1);
        when(companyRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        // When
        Page<Company> result = companiesService.list(adminActor, "1", null, false, null, null, 0, 25);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(authHelper).ensureRole(adminActor, Role.ORG_OWNER, Role.ORG_ADMIN, Role.ADMIN, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN);
        verify(authHelper).ensureOrgScope("1", adminActor);
    }

    @Test
    void shouldNormalizeKeysWhenCreatingCompany() {
        // Given
        Company company = new Company();
        company.setName("Test Company");
        company.setCompanyTypeKeys(List.of("TYPE1", "type2"));
        company.setTagKeys(List.of("TAG1", "tag2"));

        when(authHelper.validateOrg("1")).thenReturn(testOrg);
        when(validationHelper.normalizeName("Test Company")).thenReturn("testcompany");
        when(validationHelper.normalizeKeys(List.of("TYPE1", "type2"))).thenReturn(List.of("type1", "type2"));
        when(validationHelper.normalizeKeys(List.of("TAG1", "tag2"))).thenReturn(List.of("tag1", "tag2"));
        when(companyRepository.findByOrgIdAndNormalizedName(1L, "testcompany"))
                .thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        Company created = companiesService.create(company, adminActor, "1");

        // Then
        verify(validationHelper).normalizeKeys(List.of("TYPE1", "type2"));
        verify(validationHelper).normalizeKeys(List.of("TAG1", "tag2"));
    }
}

