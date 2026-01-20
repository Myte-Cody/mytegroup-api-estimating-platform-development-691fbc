package com.mytegroup.api.entity.organization;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.organization.GraphNodeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "graph_edges", indexes = {
    @Index(name = "idx_graph_edge_org_from", columnList = "org_id, from_node_type, from_node_id"),
    @Index(name = "idx_graph_edge_org_to", columnList = "org_id, to_node_type, to_node_id"),
    @Index(name = "idx_graph_edge_org_archived", columnList = "org_id, archived_at"),
    @Index(name = "idx_graph_edge_unique", columnList = "org_id, edge_type_key, from_node_type, from_node_id, to_node_type, to_node_id")
})
@Audited
@Getter
@Setter
public class GraphEdge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_node_type", nullable = false)
    private GraphNodeType fromNodeType;

    @Column(name = "from_node_id", nullable = false)
    private Long fromNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_node_type", nullable = false)
    private GraphNodeType toNodeType;

    @Column(name = "to_node_id", nullable = false)
    private Long toNodeId;

    @Column(name = "edge_type_key", nullable = false)
    private String edgeTypeKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

