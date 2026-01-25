package com.mytegroup.api.repository.organization;

import com.mytegroup.api.entity.organization.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficeRepository extends JpaRepository<Office, Long> {

    // List active offices
    List<Office> findByOrganization_IdAndArchivedAtIsNull(Long organizationId);

    // Find by normalized name
    Optional<Office> findByOrganization_IdAndNormalizedName(Long organizationId, String normalizedName);

    // Find children
    List<Office> findByOrganization_IdAndParentId(Long organizationId, Long parentId);

    // Find root offices
    List<Office> findByOrganization_IdAndParentIsNull(Long organizationId);

    // Find by tag
    @Query("SELECT DISTINCT o FROM Office o JOIN o.tagKeys t WHERE o.organization.id = :orgId AND t = :tagKey AND o.archivedAt IS NULL")
    List<Office> findByOrganization_IdAndTagKeysContaining(@Param("orgId") Long orgId, @Param("tagKey") String tagKey);

    // Find all for org (including archived)
    List<Office> findByOrganization_Id(Long organizationId);

    // Check if active exists
    boolean existsByOrganization_IdAndArchivedAtIsNull(Long organizationId);
}

