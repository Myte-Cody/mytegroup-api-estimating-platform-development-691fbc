package com.mytegroup.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_method_uri", columnList = "method, request_uri"),
    @Index(name = "idx_audit_status", columnList = "status_code"),
    @Index(name = "idx_audit_user", columnList = "username")
})
@Audited
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Audit extends BaseEntity {

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "method", length = 10, nullable = false)
    private String method;

    @Column(name = "request_uri", length = 2048, nullable = false)
    private String requestUri;

    @Column(name = "query_string", length = 2048)
    private String queryString;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "request_size")
    private Long requestSize;

    @Column(name = "response_size")
    private Long responseSize;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @PrePersist
    protected void onAuditCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}



