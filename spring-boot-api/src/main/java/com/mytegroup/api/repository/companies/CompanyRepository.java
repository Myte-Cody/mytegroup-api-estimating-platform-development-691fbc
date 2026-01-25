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
    Page<Company> findByOrganization_IdAndArchivedAtIsNull(Long organizationId, Pageable pageable);

    // Find by name (unique when not archived)
    Optional<Company> findByOrganization_IdAndNormalizedName(Long organizationId, String normalizedName);

    // Find by external ID (unique when not archived)
    Optional<Company> findByOrganization_IdAndExternalId(Long organizationId, String externalId);

    // Search by name
    List<Company> findByOrganization_IdAndNameContainingIgnoreCase(Long organizationId, String search);

    // Find by type
    @Query("SELECT DISTINCT c FROM Company c JOIN c.companyTypeKeys t WHERE c.organization.id = :orgId AND t = :typeKey AND c.archivedAt IS NULL")
    List<Company> findByOrganization_IdAndCompanyTypeKeysContaining(@Param("orgId") Long orgId, @Param("typeKey") String typeKey);

    // Find by tag
    @Query("SELECT DISTINCT c FROM Company c JOIN c.tagKeys t WHERE c.organization.id = :orgId AND t = :tagKey AND c.archivedAt IS NULL")
    List<Company> findByOrganization_IdAndTagKeysContaining(@Param("orgId") Long orgId, @Param("tagKey") String tagKey);

    // Find all for org (including archived)
    List<Company> findByOrganization_Id(Long organizationId);

    // Check if active exists
    boolean existsByOrganization_IdAndArchivedAtIsNull(Long organizationId);
}

