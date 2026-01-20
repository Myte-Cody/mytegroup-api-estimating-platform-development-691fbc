package com.mytegroup.api.repository.core;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find by email (unique)
    Optional<User> findByEmail(String email);

    // List active users
    Page<User> findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable);

    // Find by org and email
    Optional<User> findByOrgIdAndEmail(Long orgId, String email);

    // Check email exists
    boolean existsByEmail(String email);

    // Find by role
    List<User> findByOrgIdAndRolesContaining(Long orgId, Role role);

    // Find all users for org (including archived)
    List<User> findByOrgId(Long orgId);

    // Check if active exists
    boolean existsByOrgIdAndArchivedAtIsNull(Long orgId);
}

