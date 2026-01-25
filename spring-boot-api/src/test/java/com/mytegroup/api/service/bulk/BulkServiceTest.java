package com.mytegroup.api.service.bulk;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private BulkService bulkService;

    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");
    }

    @Test
    void testImportEntities_WithValidParams_ReturnsResult() {
        String entityType = "users";
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), any(), any());

        Map<String, Object> result = bulkService.importEntities(entityType, multipartFile, orgId);

        assertNotNull(result);
        assertEquals(entityType, result.get("entityType"));
        assertTrue(result.containsKey("processed"));
        assertTrue(result.containsKey("created"));
        assertTrue(result.containsKey("updated"));
        assertTrue(result.containsKey("errors"));
        verify(authHelper, times(1)).validateOrg(orgId);
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), isNull(), anyString(), isNull(), any());
    }

    @Test
    void testImportEntities_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            bulkService.importEntities("users", multipartFile, null);
        });
    }

    @Test
    void testExportEntities_WithValidParams_ReturnsBytes() {
        String entityType = "users";
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), any(), any());

        byte[] result = bulkService.exportEntities(entityType, orgId);

        assertNotNull(result);
        verify(authHelper, times(1)).validateOrg(orgId);
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), isNull(), anyString(), isNull(), any());
    }

    @Test
    void testExportEntities_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            bulkService.exportEntities("users", null);
        });
    }
}


