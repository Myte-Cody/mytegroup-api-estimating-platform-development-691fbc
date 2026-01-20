package com.mytegroup.api.entity.organization.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrgTaxonomyValue {

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "color")
    private String color;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // JSON representation

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}

