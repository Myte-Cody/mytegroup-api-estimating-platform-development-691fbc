package com.mytegroup.api.service.notifications;

import com.mytegroup.api.entity.communication.Notification;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.repository.communication.NotificationRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for notification management.
 * Handles creating and managing user notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationsService {
    
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    
    /**
     * Creates a notification
     * @throws BadRequestException if required parameters are null
     */
    @Transactional
    public Notification create(String orgId, Long userId, String type, Map<String, Object> payload) {
        if (orgId == null || userId == null || type == null) {
            throw new BadRequestException("Organization ID, User ID, and type are required");
        }
        
        Notification notification = new Notification();
        // TODO: Set organization from orgId
        // notification.setOrganization(org);
        notification.setUserId(userId.toString());
        notification.setType(type);
        // TODO: Set payload as JSON
        notification.setRead(false);
        
        Notification savedNotification = notificationRepository.save(notification);
        
        auditLogService.log(
            "notification.created",
            orgId,
            userId.toString(),
            "Notification",
            savedNotification.getId().toString(),
            payload
        );
        
        return savedNotification;
    }
    
    /**
     * Lists notifications for a user
     */
    @Transactional(readOnly = true)
    public Page<Notification> list(ActorContext actor, String orgId, Boolean read, int page, int limit) {
        if (actor.getUserId() == null) {
            throw new ForbiddenException("Missing user context");
        }
        
        if (orgId == null && actor.getRole() != com.mytegroup.api.common.enums.Role.SUPER_ADMIN) {
            throw new ForbiddenException("Missing organization context");
        }
        
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        if (resolvedOrgId == null) {
            throw new ForbiddenException("Missing organization context");
        }
        
        Long orgIdLong = Long.parseLong(resolvedOrgId);
        Long userId = Long.parseLong(actor.getUserId());
        
        Pageable pageable = PageRequest.of(page, limit);
        
        if (read != null) {
            return notificationRepository.findByOrgIdAndUserIdAndReadOrderByCreatedAtDesc(
                orgIdLong, userId, read, pageable);
        }
        return notificationRepository.findByOrgIdAndUserIdOrderByCreatedAtDesc(orgIdLong, userId, pageable);
    }
    
    /**
     * Marks a notification as read
     */
    @Transactional
    public Notification markRead(Long id, ActorContext actor, String orgId) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new com.mytegroup.api.exception.ResourceNotFoundException("Notification not found"));
        
        if (actor.getUserId() == null || 
            !notification.getUserId().equals(actor.getUserId())) {
            throw new ForbiddenException("Cannot access notification");
        }
        
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    /**
     * Gets unread count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(ActorContext actor, String orgId) {
        if (actor.getUserId() == null) {
            return 0;
        }
        
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        if (resolvedOrgId == null) {
            return 0;
        }
        
        Long orgIdLong = Long.parseLong(resolvedOrgId);
        Long userId = Long.parseLong(actor.getUserId());
        
        return notificationRepository.countByOrgIdAndUserIdAndReadFalse(orgIdLong, userId);
    }
}

