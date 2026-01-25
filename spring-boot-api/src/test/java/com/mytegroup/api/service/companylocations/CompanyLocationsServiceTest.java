package com.mytegroup.api.service.companylocations;

import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.companies.CompanyLocationRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyLocationsServiceTest {

    @Mock
    private CompanyLocationRepository companyLocationRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private ServiceValidationHelper validationHelper;

    @InjectMocks
    private CompanyLocationsService companyLocationsService;

    private Organization testOrganization;
    private Company testCompany;
    private CompanyLocation testLocation;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setName("Test Company");
        testCompany.setOrganization(testOrganization);

        testLocation = new CompanyLocation();
        testLocation.setId(1L);
        testLocation.setName("Test Location");
        testLocation.setNormalizedName("test location");
        testLocation.setCompany(testCompany);
        testLocation.setOrganization(testOrganization);
    }

    @Test
    void testCreate_WithValidLocation_CreatesLocation() {
        CompanyLocation newLocation = new CompanyLocation();
        newLocation.setName("New Location");
        newLocation.setCompany(testCompany);

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(validationHelper.normalizeName("New Location")).thenReturn("new location");
        when(companyLocationRepository.findByOrganization_IdAndCompanyIdAndNormalizedName(1L, 1L, "new location"))
            .thenReturn(Optional.empty());
        when(companyLocationRepository.save(any(CompanyLocation.class))).thenAnswer(invocation -> {
            CompanyLocation location = invocation.getArgument(0);
            location.setId(1L);
            return location;
        });

        CompanyLocation result = companyLocationsService.create(newLocation, "1");

        assertNotNull(result);
        assertEquals(testOrganization, result.getOrganization());
        verify(companyLocationRepository, times(1)).save(any(CompanyLocation.class));
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        CompanyLocation newLocation = new CompanyLocation();

        assertThrows(BadRequestException.class, () -> {
            companyLocationsService.create(newLocation, null);
        });
    }

    @Test
    void testCreate_WithNonExistentCompany_ThrowsResourceNotFoundException() {
        CompanyLocation newLocation = new CompanyLocation();
        newLocation.setCompany(testCompany);

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            companyLocationsService.create(newLocation, "1");
        });
    }

    @Test
    void testCreate_WithDuplicateName_ThrowsConflictException() {
        CompanyLocation newLocation = new CompanyLocation();
        newLocation.setName("Existing Location");
        newLocation.setCompany(testCompany);

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(validationHelper.normalizeName("Existing Location")).thenReturn("existing location");
        when(companyLocationRepository.findByOrganization_IdAndCompanyIdAndNormalizedName(1L, 1L, "existing location"))
            .thenReturn(Optional.of(testLocation));

        assertThrows(ConflictException.class, () -> {
            companyLocationsService.create(newLocation, "1");
        });
    }

    @Test
    void testList_WithValidParams_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(companyLocationRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(Page.empty());

        Page<CompanyLocation> result = companyLocationsService.list("1", 1L, null, null, false, 0, 10);

        assertNotNull(result);
        verify(companyLocationRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testGetById_WithValidId_ReturnsLocation() {
        Long locationId = 1L;
        when(companyLocationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));

        CompanyLocation result = companyLocationsService.getById(locationId, "1", false);

        assertNotNull(result);
        assertEquals(locationId, result.getId());
    }

    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long locationId = 999L;
        when(companyLocationRepository.findById(locationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            companyLocationsService.getById(locationId, "1", false);
        });
    }
}

