package com.mytegroup.api.repository.core;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

    // Find by name (unique)
    Optional<Organization> findByName(String name);

    // Find by primary domain
    Optional<Organization> findByPrimaryDomain(String domain);

    // Find by datastore type
    List<Organization> findByDatastoreType(DatastoreType type);

    // Check if any active org exists
    boolean existsByArchivedAtIsNull();

    // Find all active organizations
    List<Organization> findByArchivedAtIsNull();
}

