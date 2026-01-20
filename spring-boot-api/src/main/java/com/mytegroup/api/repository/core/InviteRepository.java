package com.mytegroup.api.repository.core;

import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.enums.core.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {

    // Find by org, email, and status
    Optional<Invite> findByOrgIdAndEmailAndStatus(Long orgId, String email, InviteStatus status);

    // Find by org, person, and status
    List<Invite> findByOrgIdAndPersonIdAndStatus(Long orgId, Long personId, InviteStatus status);

    // Find by token hash
    Optional<Invite> findByTokenHash(String tokenHash);

    // Find expired invites
    List<Invite> findByTokenExpiresBefore(LocalDateTime date);

    // Find all invites for org
    List<Invite> findByOrgId(Long orgId);

    // Find active invites for org
    List<Invite> findByOrgIdAndArchivedAtIsNull(Long orgId);
}

