package com.mytegroup.api.repository.system;

import com.mytegroup.api.entity.system.EventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    // List events
    Page<EventLog> findByOrgIdOrderByCreatedAtDesc(Long orgId, Pageable pageable);

    // Find by entity
    Page<EventLog> findByOrgIdAndEntityIdOrderByCreatedAtDesc(Long orgId, String entityId, Pageable pageable);

    // Find by action
    Page<EventLog> findByOrgIdAndActionOrderByCreatedAtDesc(Long orgId, String action, Pageable pageable);

    // Find by event type
    Page<EventLog> findByOrgIdAndEventTypeOrderByCreatedAtDesc(Long orgId, String eventType, Pageable pageable);

    // Find by entity type and entity id
    Page<EventLog> findByOrgIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
        Long orgId, 
        String entityType, 
        String entityId, 
        Pageable pageable
    );

    // Find by date range
    List<EventLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

