package com.mytegroup.api.service.sessions;

import com.mytegroup.api.service.common.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;

/**
 * Service for session management.
 * Handles Redis-based session storage and tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionsService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditLogService auditLogService;
    
    private static final String SESSION_PREFIX = "sess:";
    private static final String USER_SESSION_SET_PREFIX = "user:sessions:";
    
    /**
     * Registers a session for a user
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void registerSession(String sessionId, String userId) {
        if (sessionId == null || userId == null) {
            return;
        }
        
        String userSetKey = USER_SESSION_SET_PREFIX + userId;
        redisTemplate.opsForSet().add(userSetKey, sessionId);
        redisTemplate.expire(userSetKey, Duration.ofDays(30));
    }
    
    /**
     * Removes a session
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void removeSession(String sessionId, String userId) {
        if (sessionId == null) {
            return;
        }
        
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.delete(sessionKey);
        
        if (userId != null) {
            String userSetKey = USER_SESSION_SET_PREFIX + userId;
            redisTemplate.opsForSet().remove(userSetKey, sessionId);
        }
    }
    
    /**
     * Lists all sessions for a user
     */
    @Transactional(readOnly = true)
    public Set<Object> listUserSessions(String userId) {
        if (userId == null) {
            return Set.of();
        }
        
        String userSetKey = USER_SESSION_SET_PREFIX + userId;
        return redisTemplate.opsForSet().members(userSetKey);
    }
    
    /**
     * Removes all sessions for a user
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void removeAllUserSessions(String userId) {
        if (userId == null) {
            return;
        }
        
        Set<Object> sessionIds = listUserSessions(userId);
        for (Object sessionId : sessionIds) {
            removeSession(sessionId.toString(), userId);
        }
    }
}



