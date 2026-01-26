package com.mytegroup.api.service.migrations;

import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigrationsServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private MigrationsService migrationsService;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    @Test
    void testStart_WithValidParams_StartsMigration() {
        String orgId = "1";
        String targetDatastoreType = "DEDICATED";

        Map<String, Object> result = migrationsService.start(orgId, targetDatastoreType);

        assertNotNull(result);
        assertEquals(orgId, result.get("orgId"));
        assertEquals(targetDatastoreType, result.get("targetDatastoreType"));
        assertEquals("started", result.get("status"));
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), isNull(), anyString(), isNull(), any());
    }

    @Test
    void testStart_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            migrationsService.start(null, "DEDICATED");
        });
    }

    @Test
    void testGetStatus_WithValidOrgId_ReturnsStatus() {
        String orgId = "1";

        Map<String, Object> result = migrationsService.getStatus(orgId);

        assertNotNull(result);
        assertEquals(orgId, result.get("orgId"));
        assertTrue(result.containsKey("status"));
    }

    @Test
    void testGetStatus_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            migrationsService.getStatus(null);
        });
    }

    @Test
    void testAbort_WithValidOrgId_AbortsMigration() {
        String orgId = "1";

        Map<String, Object> result = migrationsService.abort(orgId);

        assertNotNull(result);
        assertEquals("aborted", result.get("status"));
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), isNull(), anyString(), isNull(), any());
    }

    @Test
    void testAbort_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            migrationsService.abort(null);
        });
    }
}



