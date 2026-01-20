package com.mytegroup.api.repository.cost;

import com.mytegroup.api.entity.cost.CostCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCodeRepository extends JpaRepository<CostCode, Long> {

    // Find by code (unique)
    Optional<CostCode> findByOrgIdAndCode(Long orgId, String code);

    // Find by category
    List<CostCode> findByOrgIdAndCategory(Long orgId, String category);

    // Find by active status
    List<CostCode> findByOrgIdAndActive(Long orgId, Boolean active);

    // Find by usage
    List<CostCode> findByOrgIdAndIsUsed(Long orgId, Boolean isUsed);

    // Find by import job
    List<CostCode> findByImportJobId(Long importJobId);

    // Find all for org
    List<CostCode> findByOrgId(Long orgId);
}

