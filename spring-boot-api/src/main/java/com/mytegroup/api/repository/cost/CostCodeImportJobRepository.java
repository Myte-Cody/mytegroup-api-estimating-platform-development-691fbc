package com.mytegroup.api.repository.cost;

import com.mytegroup.api.entity.cost.CostCodeImportJob;
import com.mytegroup.api.entity.enums.cost.CostCodeImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CostCodeImportJobRepository extends JpaRepository<CostCodeImportJob, Long> {

    // Find all jobs for org
    List<CostCodeImportJob> findByOrganization_Id(Long organizationId);

    // Find by status
    List<CostCodeImportJob> findByOrganization_IdAndStatus(Long organizationId, CostCodeImportStatus status);
}

