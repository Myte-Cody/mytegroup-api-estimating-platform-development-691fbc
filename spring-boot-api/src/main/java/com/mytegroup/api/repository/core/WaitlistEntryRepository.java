package com.mytegroup.api.repository.core;

import com.mytegroup.api.entity.core.WaitlistEntry;
import com.mytegroup.api.entity.enums.core.WaitlistStatus;
import com.mytegroup.api.entity.enums.core.WaitlistVerifyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    // Find by email (unique)
    Optional<WaitlistEntry> findByEmail(String email);

    // Find by status ordered by created date desc
    Page<WaitlistEntry> findByStatusOrderByCreatedAtDesc(WaitlistStatus status, Pageable pageable);

    // Find by verify status, status, ordered by created date asc
    Page<WaitlistEntry> findByVerifyStatusAndStatusOrderByCreatedAtAsc(
        WaitlistVerifyStatus verifyStatus, 
        WaitlistStatus status, 
        Pageable pageable
    );

    // Search by email
    List<WaitlistEntry> findByEmailContainingIgnoreCase(String email);

    // Find all active entries
    List<WaitlistEntry> findByArchivedAtIsNull();
}

