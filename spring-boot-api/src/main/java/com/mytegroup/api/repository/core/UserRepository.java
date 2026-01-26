package com.mytegroup.api.repository.core;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    
    // Find by username
    Optional<User> findByUsername(String username);

    // List active users
    Page<User> findByOrganization_IdAndArchivedAtIsNull(Long organizationId, Pageable pageable);

    // Find by org and email
    Optional<User> findByOrganization_IdAndEmail(Long organizationId, String email);

    // Check email exists
    boolean existsByEmail(String email);

    // Find by role
    List<User> findByOrganization_IdAndRolesContaining(Long organizationId, Role role);

    // Find all users for org (including archived)
    List<User> findByOrganization_Id(Long organizationId);

    // Check if active exists
    boolean existsByOrganization_IdAndArchivedAtIsNull(Long organizationId);

    // Find by organization ID
    @Query("SELECT u FROM User u WHERE u.organization.id = :orgId")
    List<User> findAllByOrganizationId(@Param("orgId") Long orgId);

    // ==================== Token Lookup Queries ====================

    /**
     * Find user by email verification token hash.
     * Token must not be expired and user must not be archived or on legal hold.
     */
    @Query("SELECT u FROM User u WHERE u.verificationTokenHash = :hash " +
           "AND u.verificationTokenExpires > :now " +
           "AND u.archivedAt IS NULL " +
           "AND (u.legalHold IS NULL OR u.legalHold = false)")
    Optional<User> findByVerificationTokenHash(@Param("hash") String hash, @Param("now") LocalDateTime now);

    /**
     * Find user by password reset token hash.
     * Token must not be expired and user must not be archived or on legal hold.
     */
    @Query("SELECT u FROM User u WHERE u.resetTokenHash = :hash " +
           "AND u.resetTokenExpires > :now " +
           "AND u.archivedAt IS NULL " +
           "AND (u.legalHold IS NULL OR u.legalHold = false)")
    Optional<User> findByResetTokenHash(@Param("hash") String hash, @Param("now") LocalDateTime now);

    /**
     * Clear verification token and mark email as verified.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.verificationTokenHash = null, u.verificationTokenExpires = null, " +
           "u.isEmailVerified = true WHERE u.id = :userId")
    int clearVerificationToken(@Param("userId") Long userId);

    /**
     * Set verification token for a user.
     */
    @Modifying
    @Query("UPDATE User u SET u.verificationTokenHash = :hash, u.verificationTokenExpires = :expires, " +
           "u.isEmailVerified = false WHERE u.id = :userId")
    int setVerificationToken(@Param("userId") Long userId, @Param("hash") String hash, 
                            @Param("expires") LocalDateTime expires);

    /**
     * Set password reset token for a user.
     */
    @Modifying
    @Query("UPDATE User u SET u.resetTokenHash = :hash, u.resetTokenExpires = :expires " +
           "WHERE u.id = :userId")
    int setResetToken(@Param("userId") Long userId, @Param("hash") String hash, 
                     @Param("expires") LocalDateTime expires);

    /**
     * Clear reset token and set new password hash.
     */
    @Modifying
    @Query("UPDATE User u SET u.resetTokenHash = null, u.resetTokenExpires = null, " +
           "u.passwordHash = :passwordHash WHERE u.id = :userId")
    int clearResetTokenAndSetPassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    /**
     * Update last login timestamp.
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :now WHERE u.id = :userId")
    int updateLastLogin(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // ==================== Compliance Queries ====================

    /**
     * Count users with legal hold in an org.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :orgId AND u.legalHold = true")
    long countByOrgIdAndLegalHoldTrue(@Param("orgId") Long orgId);

    /**
     * Count users with PII stripped in an org.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :orgId AND u.piiStripped = true")
    long countByOrgIdAndPiiStrippedTrue(@Param("orgId") Long orgId);

    /**
     * Find users with expired verification tokens.
     */
    @Query("SELECT u FROM User u WHERE u.verificationTokenExpires < :now AND u.verificationTokenHash IS NOT NULL")
    List<User> findUsersWithExpiredVerificationTokens(@Param("now") LocalDateTime now);

    /**
     * Find users with expired reset tokens.
     */
    @Query("SELECT u FROM User u WHERE u.resetTokenExpires < :now AND u.resetTokenHash IS NOT NULL")
    List<User> findUsersWithExpiredResetTokens(@Param("now") LocalDateTime now);
}
