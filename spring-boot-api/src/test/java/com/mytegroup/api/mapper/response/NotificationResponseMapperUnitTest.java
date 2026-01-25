package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.NotificationResponseDto;
import com.mytegroup.api.entity.communication.Notification;
import com.mytegroup.api.entity.core.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationResponseMapperUnitTest {

    private NotificationResponseMapper mapper;
    private User user;

    @BeforeEach
    void setUp() {
        mapper = new NotificationResponseMapper();
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
    }

    @Test
    void testToDtoFullEntity() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("key", "value");

        Notification entity = new Notification();
        entity.setId(10L);
        entity.setUser(user);
        entity.setType("EMAIL");
        entity.setPayload(payload);
        entity.setRead(false);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));

        // Act
        NotificationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("1", dto.getUserId());
        assertEquals("EMAIL", dto.getType());
        assertEquals(payload, dto.getMetadata());
        assertFalse(dto.getIsRead());
    }

    @Test
    void testToDoNullEntity() {
        // Act
        NotificationResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDoBuildsMapsWithNullUser() {
        // Arrange
        Notification entity = new Notification();
        entity.setId(11L);
        entity.setUser(null);
        entity.setType("SMS");
        entity.setRead(true);

        // Act
        NotificationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getUserId());
        assertEquals("SMS", dto.getType());
        assertTrue(dto.getIsRead());
    }

    @Test
    void testToDoDifferentNotificationTypes() {
        // Arrange
        String[] types = {"EMAIL", "SMS", "PUSH", "IN_APP"};

        for (String type : types) {
            Notification entity = new Notification();
            entity.setId(12L);
            entity.setUser(user);
            entity.setType(type);
            entity.setRead(false);

            // Act
            NotificationResponseDto dto = mapper.toDto(entity);

            // Assert
            assertEquals(type, dto.getType());
        }
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("template", "welcome");
        metadata.put("recipient", "user@example.com");

        Notification entity = new Notification();
        entity.setId(13L);
        entity.setUser(user);
        entity.setType("EMAIL");
        entity.setPayload(metadata);
        entity.setRead(true);
        entity.setCreatedAt(LocalDateTime.of(2024, 5, 15, 10, 30, 0));

        // Act
        NotificationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("EMAIL", dto.getType());
        assertEquals(metadata, dto.getMetadata());
        assertTrue(dto.getIsRead());
        assertEquals(LocalDateTime.of(2024, 5, 15, 10, 30, 0), dto.getCreatedAt());
    }

    @Test
    void testToDoBuildsMapsWithNullMetadata() {
        // Arrange
        Notification entity = new Notification();
        entity.setId(14L);
        entity.setUser(user);
        entity.setType("SMS");
        entity.setPayload(null);
        entity.setRead(false);

        // Act
        NotificationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getMetadata());
    }
}


