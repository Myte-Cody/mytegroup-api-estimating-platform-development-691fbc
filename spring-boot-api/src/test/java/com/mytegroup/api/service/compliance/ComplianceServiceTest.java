package com.mytegroup.api.service.compliance;

import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private ComplianceService complianceService;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    @Test
    void testStripPii_WithValidParams_StripsPii() {
        String entityType = "User";
        Long entityId = 1L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(null);

        complianceService.stripPii(entityType, entityId, orgId);

        verify(authHelper, times(1)).validateOrg(orgId);
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), isNull(), eq(entityType), eq(entityId.toString()), any());
    }

    @Test
    void testStripPii_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            complianceService.stripPii("User", 1L, null);
        });
    }

    @Test
    void testSetLegalHold_WithValidParams_SetsLegalHold() {
        String entityType = "User";
        Long entityId = 1L;
        boolean legalHold = true;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(null);

        complianceService.setLegalHold(entityType, entityId, legalHold, orgId);

        verify(authHelper, times(1)).validateOrg(orgId);
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), isNull(), eq(entityType), eq(entityId.toString()), any());
    }

    @Test
    void testSetLegalHold_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            complianceService.setLegalHold("User", 1L, true, null);
        });
    }

    @Test
    void testBatchArchive_WithValidParams_ArchivesEntities() {
        String entityType = "User";
        List<Long> entityIds = List.of(1L, 2L, 3L);
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(null);

        Map<String, Integer> result = complianceService.batchArchive(entityType, entityIds, orgId);

        assertNotNull(result);
        verify(authHelper, times(1)).validateOrg(orgId);
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), isNull(), eq(entityType), isNull(), any());
    }

    @Test
    void testBatchArchive_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            complianceService.batchArchive("User", List.of(1L), null);
        });
    }
}



