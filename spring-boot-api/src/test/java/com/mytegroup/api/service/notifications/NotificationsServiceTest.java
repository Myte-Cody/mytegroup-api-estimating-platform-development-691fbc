package com.mytegroup.api.service.notifications;

import com.mytegroup.api.entity.communication.Notification;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.communication.NotificationRepository;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationsServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationsService notificationsService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setType("test_type");
        testNotification.setRead(false);
    }

    @Test
    void testCreate_WithValidParams_CreatesNotification() {
        String orgId = "1";
        Long userId = 1L;
        String type = "test_type";
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test notification");

        Organization org = new Organization();
        org.setId(1L);
        User user = new User();
        user.setId(1L);

        when(authHelper.validateOrg(orgId)).thenReturn(org);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(1L);
            return notification;
        });

        Notification result = notificationsService.create(orgId, userId, type, payload);

        assertNotNull(result);
        assertEquals(type, result.getType());
        assertEquals(org, result.getOrganization());
        assertEquals(user, result.getUser());
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(auditLogService, times(1)).log(anyString(), eq(orgId), eq(userId.toString()), anyString(), anyString(), eq(payload));
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            notificationsService.create(null, 1L, "type", new HashMap<>());
        });
    }

    @Test
    void testCreate_WithNullUserId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            notificationsService.create("1", null, "type", new HashMap<>());
        });
    }

    @Test
    void testCreate_WithNullType_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            notificationsService.create("1", 1L, null, new HashMap<>());
        });
    }

    @Test
    void testList_WithValidParams_ReturnsPage() {
        String orgId = "1";
        Boolean read = false;
        Pageable pageable = PageRequest.of(0, 10);

        when(notificationRepository.findByOrganization_IdAndUserIdAndReadOrderByCreatedAtDesc(anyLong(), isNull(), eq(read), eq(pageable)))
            .thenReturn(Page.empty());

        Page<Notification> result = notificationsService.list(orgId, read, 0, 10);

        assertNotNull(result);
        verify(notificationRepository, times(1)).findByOrganization_IdAndUserIdAndReadOrderByCreatedAtDesc(anyLong(), isNull(), eq(read), eq(pageable));
    }

    @Test
    void testList_WithNullOrgId_ThrowsForbiddenException() {
        assertThrows(ForbiddenException.class, () -> {
            notificationsService.list(null, null, 0, 10);
        });
    }

    @Test
    void testMarkRead_WithValidId_MarksAsRead() {
        Long notificationId = 1L;
        String orgId = "1";

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationsService.markRead(notificationId, orgId);

        assertNotNull(result);
        assertTrue(result.getRead());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testMarkRead_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long notificationId = 999L;
        String orgId = "1";

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            notificationsService.markRead(notificationId, orgId);
        });
    }

    @Test
    void testGetUnreadCount_WithValidOrgId_ReturnsCount() {
        String orgId = "1";

        long result = notificationsService.getUnreadCount(orgId);

        // Currently returns 0 as userId is not available from security context
        assertEquals(0, result);
    }
}

