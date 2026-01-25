package com.mytegroup.api.service.companies;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.companylocations.CompanyLocationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompaniesImportServiceTest {

    @Mock
    private CompaniesService companiesService;

    @Mock
    private CompanyLocationsService companyLocationsService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private CompaniesImportService companiesImportService;

    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");
    }

    @Test
    void testPreviewRows_WithValidRows_ReturnsPreview() {
        String orgId = "1";
        List<Map<String, String>> rows = new ArrayList<>();
        Map<String, String> row1 = new HashMap<>();
        row1.put("name", "Company 1");
        rows.add(row1);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);

        List<Map<String, Object>> result = companiesImportService.previewRows(orgId, rows);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).containsKey("row"));
        assertTrue(result.get(0).containsKey("suggestedAction"));
        verify(authHelper, times(1)).validateOrg(orgId);
    }

    @Test
    void testPreviewRows_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            companiesImportService.previewRows(null, new ArrayList<>());
        });
    }

    @Test
    void testConfirmImport_WithValidRows_ReturnsResult() {
        String orgId = "1";
        List<Map<String, Object>> confirmedRows = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("row", 1);
        row1.put("companyAction", "create");
        row1.put("locationAction", "none");
        confirmedRows.add(row1);

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), any(), any());

        Map<String, Object> result = companiesImportService.confirmImport(orgId, confirmedRows);

        assertNotNull(result);
        assertTrue(result.containsKey("processed"));
        assertTrue(result.containsKey("companiesCreated"));
        assertTrue(result.containsKey("companiesUpdated"));
        assertTrue(result.containsKey("errors"));
        verify(authHelper, times(1)).validateOrg(orgId);
    }

    @Test
    void testConfirmImport_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            companiesImportService.confirmImport(null, new ArrayList<>());
        });
    }
}


