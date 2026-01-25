package com.mytegroup.api.service.companies;

import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.repository.companies.CompanyRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    private Organization testOrganization;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setName("Test Company");
        testCompany.setNormalizedName("test company");
        testCompany.setOrganization(testOrganization);
    }

    @Test
    void testCreate_WithValidCompany_CreatesCompany() {
        Company newCompany = new Company();
        newCompany.setName("New Company");
        newCompany.setExternalId("EXT123");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeName("New Company")).thenReturn("new company");
        when(companyRepository.findByOrganization_IdAndNormalizedName(1L, "new company"))
            .thenReturn(Optional.empty());
        when(companyRepository.findByOrganization_IdAndExternalId(1L, "EXT123"))
            .thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.setId(1L);
            return company;
        });

        Company result = companiesService.create(newCompany, "1");

        assertNotNull(result);
        assertEquals(testOrganization, result.getOrganization());
        verify(companyRepository, times(1)).save(any(Company.class));
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        Company newCompany = new Company();

        assertThrows(BadRequestException.class, () -> {
            companiesService.create(newCompany, null);
        });
    }

    @Test
    void testCreate_WithDuplicateName_ThrowsConflictException() {
        Company newCompany = new Company();
        newCompany.setName("Existing Company");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeName("Existing Company")).thenReturn("existing company");
        when(companyRepository.findByOrganization_IdAndNormalizedName(1L, "existing company"))
            .thenReturn(Optional.of(testCompany));

        assertThrows(ConflictException.class, () -> {
            companiesService.create(newCompany, "1");
        });
    }

    @Test
    void testCreate_WithDuplicateExternalId_ThrowsConflictException() {
        Company newCompany = new Company();
        newCompany.setName("New Company");
        newCompany.setExternalId("EXISTING123");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeName("New Company")).thenReturn("new company");
        when(companyRepository.findByOrganization_IdAndNormalizedName(1L, "new company"))
            .thenReturn(Optional.empty());
        when(companyRepository.findByOrganization_IdAndExternalId(1L, "EXISTING123"))
            .thenReturn(Optional.of(testCompany));

        assertThrows(ConflictException.class, () -> {
            companiesService.create(newCompany, "1");
        });
    }

    @Test
    void testList_WithValidParams_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(companyRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(Page.empty());

        Page<Company> result = companiesService.list("1", null, false, null, null, 0, 10);

        assertNotNull(result);
        verify(companyRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testList_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            companiesService.list(null, null, false, null, null, 0, 10);
        });
    }

    @Test
    void testGetById_WithValidId_ReturnsCompany() {
        Long companyId = 1L;
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));

        Company result = companiesService.getById(companyId, "1", false);

        assertNotNull(result);
        assertEquals(companyId, result.getId());
    }

    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long companyId = 999L;
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(com.mytegroup.api.exception.ResourceNotFoundException.class, () -> {
            companiesService.getById(companyId, "1", false);
        });
    }
}
