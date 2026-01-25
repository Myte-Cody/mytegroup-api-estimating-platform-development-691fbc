package com.mytegroup.api.service.sessions;

import com.mytegroup.api.service.common.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionsServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SessionsService sessionsService;

    @BeforeEach
    void setUp() {
        // Set up will be done per test
    }

    @Test
    void testRegisterSession_WithValidParams_RegistersSession() {
        String sessionId = "session-123";
        String userId = "user-1";

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        sessionsService.registerSession(sessionId, userId);

        verify(setOperations, times(1)).add(anyString(), eq(sessionId));
        verify(redisTemplate, times(1)).expire(anyString(), any(Duration.class));
    }

    @Test
    void testRegisterSession_WithNullSessionId_DoesNothing() {
        sessionsService.registerSession(null, "user-1");

        verify(setOperations, never()).add(anyString(), anyString());
    }

    @Test
    void testRegisterSession_WithNullUserId_DoesNothing() {
        sessionsService.registerSession("session-123", null);

        verify(setOperations, never()).add(anyString(), anyString());
    }

    @Test
    void testRemoveSession_WithValidParams_RemovesSession() {
        String sessionId = "session-123";
        String userId = "user-1";

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove(anyString(), anyString())).thenReturn(1L);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        sessionsService.removeSession(sessionId, userId);

        verify(redisTemplate, times(1)).delete(anyString());
        verify(setOperations, times(1)).remove(anyString(), eq(sessionId));
    }

    @Test
    void testRemoveSession_WithNullSessionId_DoesNothing() {
        sessionsService.removeSession(null, "user-1");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testListUserSessions_WithValidUserId_ReturnsSessions() {
        String userId = "user-1";
        Set<Object> sessions = Set.of("session-1", "session-2");

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(anyString())).thenReturn(sessions);

        Set<Object> result = sessionsService.listUserSessions(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testListUserSessions_WithNullUserId_ReturnsEmptySet() {
        Set<Object> result = sessionsService.listUserSessions(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRemoveAllUserSessions_WithValidUserId_RemovesAllSessions() {
        String userId = "user-1";
        Set<Object> sessions = Set.of("session-1", "session-2");

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(anyString())).thenReturn(sessions);
        when(setOperations.remove(anyString(), anyString())).thenReturn(1L);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        sessionsService.removeAllUserSessions(userId);

        verify(redisTemplate, atLeastOnce()).delete(anyString());
    }

    @Test
    void testRemoveAllUserSessions_WithNullUserId_DoesNothing() {
        sessionsService.removeAllUserSessions(null);

        verify(setOperations, never()).members(anyString());
    }
}


