package com.mytegroup.api.repository.organization;

import com.mytegroup.api.entity.organization.OrgTaxonomy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgTaxonomyRepository extends JpaRepository<OrgTaxonomy, Long> {

    // Find by namespace
    Optional<OrgTaxonomy> findByOrganization_IdAndNamespace(Long organizationId, String namespace);

    // Find all taxonomies for org
    List<OrgTaxonomy> findByOrganization_Id(Long organizationId);
}

