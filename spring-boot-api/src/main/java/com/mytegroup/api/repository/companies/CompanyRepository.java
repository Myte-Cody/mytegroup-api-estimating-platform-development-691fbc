package com.mytegroup.api.repository.companies;

import com.mytegroup.api.entity.companies.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    // List with pagination
    Page<Company> findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable);

    // Find by name (unique when not archived)
    Optional<Company> findByOrgIdAndNormalizedName(Long orgId, String normalizedName);

    // Find by external ID (unique when not archived)
    Optional<Company> findByOrgIdAndExternalId(Long orgId, String externalId);

    // Search by name
    List<Company> findByOrgIdAndNameContainingIgnoreCase(Long orgId, String search);

    // Find by type
    @Query("SELECT DISTINCT c FROM Company c JOIN c.companyTypeKeys t WHERE c.orgId = :orgId AND t = :typeKey AND c.archivedAt IS NULL")
    List<Company> findByOrgIdAndCompanyTypeKeysContaining(@Param("orgId") Long orgId, @Param("typeKey") String typeKey);

    // Find by tag
    @Query("SELECT DISTINCT c FROM Company c JOIN c.tagKeys t WHERE c.orgId = :orgId AND t = :tagKey AND c.archivedAt IS NULL")
    List<Company> findByOrgIdAndTagKeysContaining(@Param("orgId") Long orgId, @Param("tagKey") String tagKey);

    // Find all for org (including archived)
    List<Company> findByOrgId(Long orgId);

    // Check if active exists
    boolean existsByOrgIdAndArchivedAtIsNull(Long orgId);
}

