package com.mytegroup.api.entity.organization;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.embeddable.OrgTaxonomyValue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "org_taxonomies", indexes = {
    @Index(name = "idx_org_taxonomy_org_namespace", columnList = "org_id, namespace", unique = true)
})
@Audited
@Getter
@Setter
public class OrgTaxonomy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "namespace", nullable = false)
    private String namespace;

    @ElementCollection
    @CollectionTable(name = "org_taxonomy_values", joinColumns = @JoinColumn(name = "org_taxonomy_id"))
    private List<OrgTaxonomyValue> values = new ArrayList<>();
}

