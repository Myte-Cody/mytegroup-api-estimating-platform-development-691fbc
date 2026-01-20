package com.mytegroup.api.repository.projects;

import com.mytegroup.api.entity.projects.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // List with pagination
    Page<Project> findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable);

    // Find by name (unique when not archived)
    Optional<Project> findByOrgIdAndName(Long orgId, String name);

    // Find by code (unique when not archived)
    Optional<Project> findByOrgIdAndProjectCode(Long orgId, String projectCode);

    // Find by office
    List<Project> findByOrgIdAndOfficeId(Long orgId, Long officeId);

    // Search by name
    List<Project> findByOrgIdAndNameContainingIgnoreCase(Long orgId, String search);

    // Find all for org (including archived)
    List<Project> findByOrgId(Long orgId);

    // Check if active exists
    boolean existsByOrgIdAndArchivedAtIsNull(Long orgId);
}

