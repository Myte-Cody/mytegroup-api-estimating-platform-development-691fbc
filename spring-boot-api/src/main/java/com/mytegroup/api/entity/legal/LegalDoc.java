package com.mytegroup.api.entity.legal;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.enums.legal.LegalDocType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_docs", indexes = {
    @Index(name = "idx_legal_doc_type_version", columnList = "type, version", unique = true),
    @Index(name = "idx_legal_doc_type_effective", columnList = "type, effective_at DESC, created_at DESC")
})
@Audited
@Getter
@Setter
public class LegalDoc extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LegalDocType type;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt = LocalDateTime.now();

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}

