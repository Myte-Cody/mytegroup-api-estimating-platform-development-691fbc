package com.mytegroup.api.entity.system;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "event_logs", indexes = {
    @Index(name = "idx_event_log_event_type", columnList = "event_type"),
    @Index(name = "idx_event_log_action", columnList = "action"),
    @Index(name = "idx_event_log_entity_type", columnList = "entity_type"),
    @Index(name = "idx_event_log_entity_id", columnList = "entity_id"),
    @Index(name = "idx_event_log_actor", columnList = "actor"),
    @Index(name = "idx_event_log_org_created", columnList = "org_id, created_at DESC"),
    @Index(name = "idx_event_log_org_entity_created", columnList = "org_id, entity_id, created_at DESC"),
    @Index(name = "idx_event_log_org_action_created", columnList = "org_id, action, created_at DESC"),
    @Index(name = "idx_event_log_org_event_type_created", columnList = "org_id, event_type, created_at DESC")
})
@Audited
@Getter
@Setter
public class EventLog extends BaseEntity {

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "action")
    private String action;

    @Column(name = "entity")
    private String entity;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "actor")
    private String actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;

    // Note: EventLog extends BaseEntity but only uses createdAt (not updatedAt)
    // The updatedAt field from BaseEntity will exist but won't be used
}

