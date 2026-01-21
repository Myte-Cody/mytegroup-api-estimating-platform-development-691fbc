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
    
    // Find by org and status
    List<Invite> findByOrgIdAndStatus(Long orgId, InviteStatus status);
    
    // Find pending invites that have expired
    @Query("SELECT i FROM Invite i WHERE i.organization.id = :orgId AND i.status = 'PENDING' AND i.tokenExpires <= :now")
    List<Invite> findExpiredPendingInvites(@Param("orgId") Long orgId, @Param("now") LocalDateTime now);
    
    // Count recent invites for throttling
    @Query("SELECT COUNT(i) FROM Invite i WHERE i.organization.id = :orgId AND i.email = :email AND i.createdAt >= :since")
    long countRecentInvites(@Param("orgId") Long orgId, @Param("email") String email, @Param("since") LocalDateTime since);
    
    // Find pending active invite
    @Query("SELECT i FROM Invite i WHERE i.organization.id = :orgId AND i.email = :email " +
           "AND i.status = 'PENDING' AND i.tokenExpires > :now AND i.archivedAt IS NULL")
    Optional<Invite> findPendingActiveInvite(@Param("orgId") Long orgId, @Param("email") String email, 
                                             @Param("now") LocalDateTime now);
}

