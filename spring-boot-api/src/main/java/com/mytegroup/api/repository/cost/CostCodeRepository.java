package com.mytegroup.api.repository.cost;

import com.mytegroup.api.entity.cost.CostCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCodeRepository extends JpaRepository<CostCode, Long>, JpaSpecificationExecutor<CostCode> {

    // Find by code (unique)
    Optional<CostCode> findByOrganization_IdAndCode(Long organizationId, String code);

    // Find by category
    List<CostCode> findByOrganization_IdAndCategory(Long organizationId, String category);

    // Find by active status
    List<CostCode> findByOrganization_IdAndActive(Long organizationId, Boolean active);

    // Find by usage
    List<CostCode> findByOrganization_IdAndIsUsed(Long organizationId, Boolean isUsed);

    // Find by import job
    List<CostCode> findByImportJobId(Long importJobId);

    // Find all for org
    List<CostCode> findByOrganization_Id(Long organizationId);
}

