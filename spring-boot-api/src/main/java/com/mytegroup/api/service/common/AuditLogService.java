package com.mytegroup.api.service.common;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.system.EventLog;
import com.mytegroup.api.repository.core.OrganizationRepository;
import com.mytegroup.api.repository.system.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for audit logging.
 * Uses REQUIRES_NEW propagation to ensure audit records persist even if main transaction fails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final EventLogRepository eventLogRepository;
    private final OrganizationRepository organizationRepository;
    
    /**
     * Logs an event with the given parameters
     * Uses REQUIRES_NEW propagation to ensure audit persists even if main transaction rolls back
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String orgId, String userId, String entity, String entityId, Map<String, Object> metadata) {
        try {
            EventLog eventLog = new EventLog();
            eventLog.setEventType(eventType);
            eventLog.setEntity(entity);
            eventLog.setEntityType(entity);
            eventLog.setEntityId(entityId);
            eventLog.setUserId(userId);
            eventLog.setActor(userId);
            
            // Derive action from eventType (e.g., "project.created" -> "created")
            if (eventType != null && eventType.contains(".")) {
                String[] parts = eventType.split("\\.");
                eventLog.setAction(parts[parts.length - 1]);
            } else {
                eventLog.setAction(eventType);
            }
            
            // Set organization if orgId provided
            if (orgId != null) {
                try {
                    Long orgIdLong = Long.parseLong(orgId);
                    organizationRepository.findById(orgIdLong)
                        .ifPresent(eventLog::setOrganization);
                } catch (NumberFormatException e) {
                    // orgId might be a string ID, try to find by other means or skip
                    log.debug("Could not parse orgId as Long for audit log: {}", orgId);
                }
            }
            
            eventLog.setMetadata(metadata != null ? metadata : new HashMap<>());
            // createdAt is automatically set by BaseEntity @PrePersist
            
            eventLogRepository.save(eventLog);
            
            if (log.isDebugEnabled()) {
                log.debug("Audit log created: eventType={}, entity={}, entityId={}", eventType, entity, entityId);
            }
        } catch (Exception e) {
            // Don't fail the main transaction if audit logging fails
            log.error("Failed to persist audit log for eventType: {}", eventType, e);
        }
    }
    
    /**
     * Logs a mutation event (create, update, delete, etc.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logMutation(String action, String entity, String entityId, String orgId, String userId, Map<String, Object> metadata, Map<String, Object> payload) {
        String eventType = entity + "." + action;
        log(eventType, orgId, userId, entity, entityId, metadata);
    }
    
    /**
     * Logs a simple event string
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType) {
        log(eventType, null, null, null, null, null);
    }
    
    /**
     * Logs an event with a metadata map
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, Map<String, Object> metadata) {
        log(eventType, null, null, null, null, metadata);
    }
    
    /**
     * Logs an event with orgId and userId
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String orgId, String userId) {
        log(eventType, orgId, userId, null, null, null);
    }
}

