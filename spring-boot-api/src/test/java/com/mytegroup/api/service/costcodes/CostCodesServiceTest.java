package com.mytegroup.api.service.costcodes;

import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.cost.CostCodeRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
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
class CostCodesServiceTest {

    @Mock
    private CostCodeRepository costCodeRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private CostCodesService costCodesService;

    private Organization testOrganization;
    private CostCode testCostCode;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testCostCode = new CostCode();
        testCostCode.setId(1L);
        testCostCode.setCode("CC001");
        testCostCode.setCategory("Labor");
        testCostCode.setDescription("Test Cost Code");
        testCostCode.setOrganization(testOrganization);
    }

    @Test
    void testCreate_WithValidCostCode_CreatesCostCode() {
        CostCode newCostCode = new CostCode();
        newCostCode.setCode("CC002");
        newCostCode.setCategory("Material");
        newCostCode.setDescription("New Cost Code");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(costCodeRepository.findByOrganization_IdAndCode(1L, "CC002"))
            .thenReturn(Optional.empty());
        when(costCodeRepository.save(any(CostCode.class))).thenAnswer(invocation -> {
            CostCode costCode = invocation.getArgument(0);
            costCode.setId(1L);
            return costCode;
        });

        CostCode result = costCodesService.create(newCostCode, "1");

        assertNotNull(result);
        assertEquals(testOrganization, result.getOrganization());
        verify(costCodeRepository, times(1)).save(any(CostCode.class));
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        CostCode newCostCode = new CostCode();

        assertThrows(BadRequestException.class, () -> {
            costCodesService.create(newCostCode, null);
        });
    }

    @Test
    void testCreate_WithEmptyCategory_ThrowsBadRequestException() {
        CostCode newCostCode = new CostCode();
        newCostCode.setCategory("   ");
        newCostCode.setCode("CC001");
        newCostCode.setDescription("Description");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);

        assertThrows(BadRequestException.class, () -> {
            costCodesService.create(newCostCode, "1");
        });
    }

    @Test
    void testCreate_WithEmptyCode_ThrowsBadRequestException() {
        CostCode newCostCode = new CostCode();
        newCostCode.setCategory("Labor");
        newCostCode.setCode("   ");
        newCostCode.setDescription("Description");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);

        assertThrows(BadRequestException.class, () -> {
            costCodesService.create(newCostCode, "1");
        });
    }

    @Test
    void testCreate_WithEmptyDescription_ThrowsBadRequestException() {
        CostCode newCostCode = new CostCode();
        newCostCode.setCategory("Labor");
        newCostCode.setCode("CC001");
        newCostCode.setDescription("   ");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);

        assertThrows(BadRequestException.class, () -> {
            costCodesService.create(newCostCode, "1");
        });
    }

    @Test
    void testCreate_WithDuplicateCode_ThrowsConflictException() {
        CostCode newCostCode = new CostCode();
        newCostCode.setCode("CC001");
        newCostCode.setCategory("Labor");
        newCostCode.setDescription("Description");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(costCodeRepository.findByOrganization_IdAndCode(1L, "CC001"))
            .thenReturn(Optional.of(testCostCode));

        assertThrows(ConflictException.class, () -> {
            costCodesService.create(newCostCode, "1");
        });
    }

    @Test
    void testList_WithValidParams_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(costCodeRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(Page.empty());

        Page<CostCode> result = costCodesService.list("1", null, false, 0, 10);

        assertNotNull(result);
        verify(costCodeRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testList_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            costCodesService.list(null, null, false, 0, 10);
        });
    }

    @Test
    void testGetById_WithValidId_ReturnsCostCode() {
        Long costCodeId = 1L;
        when(costCodeRepository.findById(costCodeId)).thenReturn(Optional.of(testCostCode));

        CostCode result = costCodesService.getById(costCodeId, "1");

        assertNotNull(result);
        assertEquals(costCodeId, result.getId());
    }

    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long costCodeId = 999L;
        when(costCodeRepository.findById(costCodeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            costCodesService.getById(costCodeId, "1");
        });
    }

    @Test
    void testGetById_WithWrongOrg_ThrowsForbiddenException() {
        Long costCodeId = 1L;
        when(costCodeRepository.findById(costCodeId)).thenReturn(Optional.of(testCostCode));

        assertThrows(ForbiddenException.class, () -> {
            costCodesService.getById(costCodeId, "999");
        });
    }
}

