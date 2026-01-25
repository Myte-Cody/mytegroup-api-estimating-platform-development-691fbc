package com.mytegroup.api.service.ingestion;

import com.mytegroup.api.exception.ServiceUnavailableException;
import com.mytegroup.api.service.common.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class IngestionContactsServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private IngestionContactsService ingestionContactsService;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    @Test
    void testSuggestMapping_WithValidHeaders_ReturnsMapping() {
        String orgId = "1";
        List<String> headers = List.of("Email", "Phone", "Display Name");
        List<Map<String, Object>> sampleRows = new java.util.ArrayList<>();

        Map<String, Object> result = ingestionContactsService.suggestMapping(headers, sampleRows, orgId);

        assertNotNull(result);
        assertTrue(result.containsKey("mapping"));
        Map<String, String> mapping = (Map<String, String>) result.get("mapping");
        assertTrue(mapping.containsKey("email"));
        assertTrue(mapping.containsKey("phone"));
        assertTrue(mapping.containsKey("displayName"));
    }

    @Test
    void testParseRow_WithValidRow_ReturnsParsed() {
        String orgId = "1";
        Map<String, Object> row = new HashMap<>();
        row.put("Email", "test@example.com");
        row.put("Phone", "+15145551234");
        Map<String, String> mapping = new HashMap<>();
        mapping.put("email", "Email");
        mapping.put("phone", "Phone");

        Map<String, Object> result = ingestionContactsService.parseRow(row, mapping, orgId);

        assertNotNull(result);
        assertTrue(result.containsKey("row"));
        assertTrue(result.containsKey("mapping"));
    }

    @Test
    void testEnrich_WithValidData_ThrowsServiceUnavailableException() {
        String orgId = "1";
        Map<String, Object> contactData = new HashMap<>();

        assertThrows(ServiceUnavailableException.class, () -> {
            ingestionContactsService.enrich(contactData, orgId);
        });
    }
}


