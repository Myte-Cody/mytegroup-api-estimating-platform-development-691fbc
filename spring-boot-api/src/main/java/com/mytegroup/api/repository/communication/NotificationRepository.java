package com.mytegroup.api.repository.communication;

import com.mytegroup.api.entity.communication.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find by org, user, read status, ordered by created date desc
    Page<Notification> findByOrganization_IdAndUserIdAndReadOrderByCreatedAtDesc(Long organizationId, Long userId, Boolean read, Pageable pageable);

    // All for user ordered by created date desc
    Page<Notification> findByOrganization_IdAndUserIdOrderByCreatedAtDesc(Long organizationId, Long userId, Pageable pageable);

    // Count unread
    long countByOrganization_IdAndUserIdAndReadFalse(Long organizationId, Long userId);
}

