package com.mytegroup.api.repository.companies;

import com.mytegroup.api.entity.companies.CompanyLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, Long>, JpaSpecificationExecutor<CompanyLocation> {

    // Find by org, company, and not archived
    List<CompanyLocation> findByOrganization_IdAndCompanyIdAndArchivedAtIsNull(Long organizationId, Long companyId);

    // Find by name (unique when not archived)
    Optional<CompanyLocation> findByOrganization_IdAndCompanyIdAndNormalizedName(Long organizationId, Long companyId, String normalizedName);

    // Find by external ID
    Optional<CompanyLocation> findByOrganization_IdAndCompanyIdAndExternalId(Long organizationId, Long companyId, String externalId);

    // Find all locations for company
    List<CompanyLocation> findByCompanyId(Long companyId);

    // Find by tag
    @Query("SELECT DISTINCT cl FROM CompanyLocation cl JOIN cl.tagKeys t WHERE cl.organization.id = :orgId AND t = :tagKey AND cl.archivedAt IS NULL")
    List<CompanyLocation> findByOrganization_IdAndTagKeysContaining(@Param("orgId") Long orgId, @Param("tagKey") String tagKey);

    // Find all for org (including archived)
    List<CompanyLocation> findByOrganization_Id(Long organizationId);

    // Check if active exists
    boolean existsByOrganization_IdAndArchivedAtIsNull(Long organizationId);
}

