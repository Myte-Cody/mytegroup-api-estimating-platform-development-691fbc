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
    List<Office> findByOrgIdAndArchivedAtIsNull(Long orgId);

    // Find by normalized name
    Optional<Office> findByOrgIdAndNormalizedName(Long orgId, String normalizedName);

    // Find children
    List<Office> findByOrgIdAndParentOrgLocationId(Long orgId, Long parentId);

    // Find root offices
    List<Office> findByOrgIdAndParentOrgLocationIdIsNull(Long orgId);

    // Find by tag
    @Query("SELECT DISTINCT o FROM Office o JOIN o.tagKeys t WHERE o.orgId = :orgId AND t = :tagKey AND o.archivedAt IS NULL")
    List<Office> findByOrgIdAndTagKeysContaining(@Param("orgId") Long orgId, @Param("tagKey") String tagKey);

    // Find all for org (including archived)
    List<Office> findByOrgId(Long orgId);

    // Check if active exists
    boolean existsByOrgIdAndArchivedAtIsNull(Long orgId);
}

