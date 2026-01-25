package com.mytegroup.api.repository.projects;

import com.mytegroup.api.entity.projects.Project;
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
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    // List with pagination
    Page<Project> findByOrganization_IdAndArchivedAtIsNull(Long organizationId, Pageable pageable);

    // Find by name (unique when not archived)
    Optional<Project> findByOrganization_IdAndName(Long organizationId, String name);

    // Find by code (unique when not archived)
    Optional<Project> findByOrganization_IdAndProjectCode(Long organizationId, String projectCode);

    // Find by office
    List<Project> findByOrganization_IdAndOfficeId(Long organizationId, Long officeId);

    // Search by name
    List<Project> findByOrganization_IdAndNameContainingIgnoreCase(Long organizationId, String search);

    // Find all for org (including archived)
    List<Project> findByOrganization_Id(Long organizationId);

    // Check if active exists
    boolean existsByOrganization_IdAndArchivedAtIsNull(Long organizationId);
}

