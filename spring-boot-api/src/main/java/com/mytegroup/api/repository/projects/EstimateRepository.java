package com.mytegroup.api.repository.projects;

import com.mytegroup.api.entity.projects.Estimate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstimateRepository extends JpaRepository<Estimate, Long> {

    // Find by org, project, and not archived
    List<Estimate> findByOrganization_IdAndProjectIdAndArchivedAtIsNull(Long organizationId, Long projectId);

    // Find by name (unique when not archived)
    Optional<Estimate> findByOrganization_IdAndProjectIdAndName(Long organizationId, Long projectId, String name);

    // Find all estimates for project
    List<Estimate> findByProjectId(Long projectId);

    // Find by creator
    List<Estimate> findByCreatedByUserId(Long userId);

    // Find all for org (including archived)
    List<Estimate> findByOrganization_Id(Long organizationId);

    // Check if active exists
    boolean existsByOrganization_IdAndArchivedAtIsNull(Long organizationId);
}

