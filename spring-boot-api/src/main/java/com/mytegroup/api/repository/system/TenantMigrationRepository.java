package com.mytegroup.api.repository.system;

import com.mytegroup.api.entity.enums.system.MigrationStatus;
import com.mytegroup.api.entity.system.TenantMigration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantMigrationRepository extends JpaRepository<TenantMigration, Long> {

    // Find migrations for org
    List<TenantMigration> findByOrganization_Id(Long organizationId);

    // Find by status
    List<TenantMigration> findByOrganization_IdAndStatus(Long organizationId, MigrationStatus status);

    // Find all by status (admin)
    List<TenantMigration> findByStatus(MigrationStatus status);
}

