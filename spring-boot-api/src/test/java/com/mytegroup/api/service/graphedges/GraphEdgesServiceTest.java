package com.mytegroup.api.service.graphedges;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import com.mytegroup.api.entity.organization.GraphEdge;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.organization.GraphEdgeRepository;
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
class GraphEdgesServiceTest {

    @Mock
    private GraphEdgeRepository graphEdgeRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private ServiceValidationHelper validationHelper;

    @InjectMocks
    private GraphEdgesService graphEdgesService;

    private Organization testOrganization;
    private GraphEdge testEdge;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testEdge = new GraphEdge();
        testEdge.setId(1L);
        testEdge.setEdgeTypeKey("reports_to");
        testEdge.setFromNodeType(GraphNodeType.PERSON);
        testEdge.setToNodeType(GraphNodeType.PERSON);
        testEdge.setOrganization(testOrganization);
    }

    @Test
    void testCreate_WithValidEdge_CreatesEdge() {
        GraphEdge newEdge = new GraphEdge();
        newEdge.setEdgeTypeKey("reports_to");
        newEdge.setFromNodeType(GraphNodeType.PERSON);
        newEdge.setToNodeType(GraphNodeType.PERSON);

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeKey("reports_to")).thenReturn("reports_to");
        when(graphEdgeRepository.save(any(GraphEdge.class))).thenAnswer(invocation -> {
            GraphEdge edge = invocation.getArgument(0);
            edge.setId(1L);
            return edge;
        });
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), anyString(), any());

        GraphEdge result = graphEdgesService.create(newEdge, "1");

        assertNotNull(result);
        assertEquals(testOrganization, result.getOrganization());
        verify(graphEdgeRepository, times(1)).save(any(GraphEdge.class));
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        GraphEdge newEdge = new GraphEdge();

        assertThrows(BadRequestException.class, () -> {
            graphEdgesService.create(newEdge, null);
        });
    }

    @Test
    void testCreate_WithEmptyEdgeTypeKey_ThrowsBadRequestException() {
        GraphEdge newEdge = new GraphEdge();
        newEdge.setEdgeTypeKey("   ");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeKey("   ")).thenReturn("");

        assertThrows(BadRequestException.class, () -> {
            graphEdgesService.create(newEdge, "1");
        });
    }

    @Test
    void testList_WithValidParams_ReturnsList() {
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(graphEdgeRepository.findByOrganization_IdAndArchivedAtIsNull(1L))
            .thenReturn(List.of(testEdge));

        List<GraphEdge> result = graphEdgesService.list("1", null);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testList_WithEdgeTypeKey_ReturnsFilteredList() {
        String edgeTypeKey = "reports_to";
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(graphEdgeRepository.findByOrganization_IdAndEdgeTypeKey(1L, edgeTypeKey))
            .thenReturn(List.of(testEdge));

        List<GraphEdge> result = graphEdgesService.list("1", edgeTypeKey);

        assertNotNull(result);
        verify(graphEdgeRepository, times(1)).findByOrganization_IdAndEdgeTypeKey(1L, edgeTypeKey);
    }

    @Test
    void testList_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            graphEdgesService.list(null, null);
        });
    }

    @Test
    void testDelete_WithValidId_DeletesEdge() {
        Long edgeId = 1L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(graphEdgeRepository.findById(edgeId)).thenReturn(Optional.of(testEdge));
        when(graphEdgeRepository.save(any(GraphEdge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), anyString(), any());

        graphEdgesService.delete(edgeId, orgId);

        verify(graphEdgeRepository, times(1)).save(any(GraphEdge.class));
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), any(), anyString(), anyString(), any());
    }

    @Test
    void testDelete_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long edgeId = 999L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(graphEdgeRepository.findById(edgeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            graphEdgesService.delete(edgeId, orgId);
        });
    }
}


