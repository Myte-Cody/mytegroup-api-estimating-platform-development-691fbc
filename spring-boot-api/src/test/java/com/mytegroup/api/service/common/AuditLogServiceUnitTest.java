package com.mytegroup.api.service.common;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.system.EventLog;
import com.mytegroup.api.repository.core.OrganizationRepository;
import com.mytegroup.api.repository.system.EventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceUnitTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private Organization testOrganization;
    private Map<String, Object> testMetadata;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");

        testMetadata = new HashMap<>();
        testMetadata.put("key1", "value1");
        testMetadata.put("key2", "value2");
    }

    @Test
    void testLogFullEvent() {
        // Arrange
        String eventType = "company.created";
        String orgId = "1";
        String userId = "user123";
        String entity = "Company";
        String entityId = "comp456";

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log(eventType, orgId, userId, entity, entityId, testMetadata);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("company.created", savedLog.getEventType());
        assertEquals("created", savedLog.getAction());
        assertEquals("user123", savedLog.getUserId());
        assertEquals("Company", savedLog.getEntity());
        assertEquals("comp456", savedLog.getEntityId());
        assertEquals(testOrganization, savedLog.getOrganization());
        assertEquals(testMetadata, savedLog.getMetadata());
    }

    @Test
    void testLogWithNullMetadata() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("test.event", "1", "user123", "Entity", "ent123", null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertNotNull(savedLog.getMetadata());
        assertTrue(savedLog.getMetadata().isEmpty());
    }

    @Test
    void testLogExtractsActionFromEventType() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("project.updated", null, null, "Project", "proj789", null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("updated", savedLog.getAction());
    }

    @Test
    void testLogWithSimpleEventType() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("simple_event", null, null, null, null, null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("simple_event", savedLog.getAction());
    }

    @Test
    void testLogWithInvalidOrgId() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("test.event", "invalid_org_id", "user123", "Entity", "ent123", null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        // Organization should be null since orgId is invalid
        EventLog savedLog = captor.getValue();
        assertNull(savedLog.getOrganization());
    }

    @Test
    void testLogWithNullOrgId() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("test.event", null, "user123", "Entity", "ent123", null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertNull(savedLog.getOrganization());
    }

    @Test
    void testLogMutation() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.logMutation("created", "Company", "comp123", "1", "user456", testMetadata, null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("Company.created", savedLog.getEventType());
        assertEquals("created", savedLog.getAction());
    }

    @Test
    void testLogSimpleEventType() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("application.started");

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("application.started", savedLog.getEventType());
        assertNull(savedLog.getUserId());
        assertNull(savedLog.getOrganization());
    }

    @Test
    void testLogWithMetadataOnly() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("test.event", testMetadata);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("test.event", savedLog.getEventType());
        assertEquals(testMetadata, savedLog.getMetadata());
    }

    @Test
    void testLogWithOrgIdAndUserId() {
        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("test.event", "1", "user789");

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("test.event", savedLog.getEventType());
        assertEquals("user789", savedLog.getUserId());
        assertEquals(testOrganization, savedLog.getOrganization());
    }

    @Test
    void testLogSetsUserIdAndActor() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("test.event", null, "userXYZ", "Entity", "ent123", null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("userXYZ", savedLog.getUserId());
        assertEquals("userXYZ", savedLog.getActor());
    }

    @Test
    void testLogSetsEntityAndEntityType() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("test.event", null, null, "Company", "comp123", null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        assertEquals("Company", savedLog.getEntity());
        assertEquals("Company", savedLog.getEntityType());
    }

    @Test
    void testLogWithComplexEventTypeName() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("estimate.line_item.updated", null, null, "LineItem", "li456", null);

        // Assert
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        
        EventLog savedLog = captor.getValue();
        // Should extract the last part after the last dot
        assertEquals("updated", savedLog.getAction());
    }

    @Test
    void testLogDoesNotThrowExceptionOnRepositorySaveFailure() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> 
            auditLogService.log("test.event", "1", "user123", "Entity", "ent123", testMetadata)
        );
    }

    @Test
    void testLogRepositoryIsCalled() {
        // Arrange
        when(eventLogRepository.save(any(EventLog.class))).thenReturn(new EventLog());

        // Act
        auditLogService.log("test.event", null, null, null, null, null);

        // Assert
        verify(eventLogRepository, times(1)).save(any(EventLog.class));
    }
}


