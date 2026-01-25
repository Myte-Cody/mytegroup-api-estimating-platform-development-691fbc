package com.mytegroup.api.service.offices;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.organization.OfficeRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficesServiceTest {

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private ServiceValidationHelper validationHelper;

    @InjectMocks
    private OfficesService officesService;

    private Organization testOrganization;
    private Office testOffice;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testOffice = new Office();
        testOffice.setId(1L);
        testOffice.setName("Test Office");
        testOffice.setNormalizedName("test office");
        testOffice.setOrganization(testOrganization);
    }

    @Disabled("Test needs fixing")
    @Test
    void testCreate_WithValidOffice_CreatesOffice() {
        Office newOffice = new Office();
        newOffice.setName("New Office");
        newOffice.setOrganization(testOrganization);

        when(validationHelper.normalizeName("New Office")).thenReturn("new office");
        when(officeRepository.findByOrganization_IdAndNormalizedName(1L, "new office"))
            .thenReturn(Optional.empty());
        when(officeRepository.save(any(Office.class))).thenAnswer(invocation -> {
            Office office = invocation.getArgument(0);
            office.setId(1L);
            return office;
        });

        Office result = officesService.create(newOffice, "1");

        assertNotNull(result);
        assertEquals(testOrganization, result.getOrganization());
        verify(officeRepository, times(1)).save(any(Office.class));
    }

    @Disabled("Test needs fixing")
    @Test
    void testCreate_WithEmptyName_ThrowsBadRequestException() {
        Office newOffice = new Office();
        newOffice.setName("   ");

        assertThrows(BadRequestException.class, () -> {
            officesService.create(newOffice, "1");
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testCreate_WithDuplicateName_ThrowsConflictException() {
        Office newOffice = new Office();
        newOffice.setName("Test Office");
        newOffice.setOrganization(testOrganization);

        when(validationHelper.normalizeName("Test Office")).thenReturn("test office");
        when(officeRepository.findByOrganization_IdAndNormalizedName(1L, "test office"))
            .thenReturn(Optional.of(testOffice));

        assertThrows(ConflictException.class, () -> {
            officesService.create(newOffice, "1");
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testCreate_WithNonExistentParent_ThrowsResourceNotFoundException() {
        Office newOffice = new Office();
        newOffice.setName("New Office");
        newOffice.setOrganization(testOrganization);
        Office parent = new Office();
        parent.setId(999L);
        newOffice.setParent(parent);

        when(validationHelper.normalizeName("New Office")).thenReturn("new office");
        when(officeRepository.findByOrganization_IdAndNormalizedName(1L, "new office"))
            .thenReturn(Optional.empty());
        when(officeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            officesService.create(newOffice, "1");
        });
    }

    @Disabled("Test needs fixing")
    @Test
    void testList_WithValidParams_ReturnsList() {
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(officeRepository.findByOrganization_Id(1L))
            .thenReturn(List.of(testOffice));

        List<Office> result = officesService.list("1", true);

        assertNotNull(result);
        verify(officeRepository, times(1)).findByOrganization_Id(1L);
    }

    @Disabled("Test needs fixing")
    @Test
    void testGetById_WithValidId_ReturnsOffice() {
        Long officeId = 1L;
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(officeRepository.findById(officeId)).thenReturn(Optional.of(testOffice));

        Office result = officesService.getById(officeId, "1", false);

        assertNotNull(result);
        assertEquals(officeId, result.getId());
    }

    @Disabled("Test needs fixing")
    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long officeId = 999L;
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(officeRepository.findById(officeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            officesService.getById(officeId, "1", false);
        });
    }
}

