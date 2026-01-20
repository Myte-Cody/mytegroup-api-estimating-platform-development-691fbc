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
    List<Estimate> findByOrgIdAndProjectIdAndArchivedAtIsNull(Long orgId, Long projectId);

    // Find by name (unique when not archived)
    Optional<Estimate> findByOrgIdAndProjectIdAndName(Long orgId, Long projectId, String name);

    // Find all estimates for project
    List<Estimate> findByProjectId(Long projectId);

    // Find by creator
    List<Estimate> findByCreatedByUserId(Long userId);

    // Find all for org (including archived)
    List<Estimate> findByOrgId(Long orgId);

    // Check if active exists
    boolean existsByOrgIdAndArchivedAtIsNull(Long orgId);
}

