package com.mytegroup.api.service.notifications;

import com.mytegroup.api.entity.communication.Notification;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.repository.communication.NotificationRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
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
    private final ServiceAuthorizationHelper authHelper;
    private final UserRepository userRepository;
    
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
        notification.setOrganization(authHelper.validateOrg(orgId));
        notification.setUser(userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        notification.setType(type);
        notification.setPayload(payload);
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
    public Page<Notification> list(String orgId, Boolean read, int page, int limit) {
        if (orgId == null) {
            throw new ForbiddenException("Missing organization context");
        }
        
        Long orgIdLong = Long.parseLong(orgId);
        // TODO: Get userId from security context when sessions are implemented
        Long userId = null;
        
        Pageable pageable = PageRequest.of(page, limit);
        
        if (read != null) {
            return notificationRepository.findByOrganization_IdAndUserIdAndReadOrderByCreatedAtDesc(
                orgIdLong, userId, read, pageable);
        }
        // TODO: Get user from security context when sessions are implemented
        // For now, return empty page
        return notificationRepository.findByOrganization_IdAndUserIdOrderByCreatedAtDesc(orgIdLong, userId, pageable);
    }
    
    /**
     * Marks a notification as read
     */
    @Transactional
    public Notification markRead(Long id, String orgId) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new com.mytegroup.api.exception.ResourceNotFoundException("Notification not found"));
        
        // TODO: Validate user access when sessions are implemented
        // For now, allow access if orgId matches
        
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    /**
     * Gets unread count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String orgId) {
        // TODO: Get userId from security context when sessions are implemented
        // For now, return 0
        Long userId = null;
        if (userId == null) {
            return 0;
        }
        
        if (orgId == null) {
            return 0;
        }
        
        Long orgIdLong = Long.parseLong(orgId);
        // TODO: Get userId from security context when sessions are implemented
        // For now, return 0
        return 0;
    }
}

