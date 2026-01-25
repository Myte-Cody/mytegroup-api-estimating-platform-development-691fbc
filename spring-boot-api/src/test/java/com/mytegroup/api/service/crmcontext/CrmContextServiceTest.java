package com.mytegroup.api.service.crmcontext;

import com.mytegroup.api.exception.BadRequestException;
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
class CrmContextServiceTest {

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private CrmContextService crmContextService;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    @Test
    void testListDocuments_WithValidParams_ReturnsDocuments() {
        String orgId = "1";
        String entityType = "Person";
        String entityId = "1";
        int page = 0;
        int limit = 10;

        when(authHelper.validateOrg(orgId)).thenReturn(null);

        Map<String, Object> result = crmContextService.listDocuments(orgId, entityType, entityId, page, limit);

        assertNotNull(result);
        assertTrue(result.containsKey("data"));
        assertTrue(result.containsKey("total"));
        assertEquals(page, result.get("page"));
        assertEquals(limit, result.get("limit"));
        verify(authHelper, times(1)).validateOrg(orgId);
    }

    @Test
    void testListDocuments_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            crmContextService.listDocuments(null, "Person", "1", 0, 10);
        });
    }

    @Test
    void testIndexDocument_WithValidParams_IndexesDocument() {
        String orgId = "1";
        String entityType = "Person";
        String entityId = "1";
        String title = "Test Document";
        String text = "Test content";
        Map<String, Object> metadata = Map.of("source", "manual");

        when(authHelper.validateOrg(orgId)).thenReturn(null);

        crmContextService.indexDocument(orgId, entityType, entityId, title, text, metadata);

        verify(authHelper, times(1)).validateOrg(orgId);
    }

    @Test
    void testIndexDocument_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            crmContextService.indexDocument(null, "Person", "1", "Title", "Text", Map.of());
        });
    }
}

