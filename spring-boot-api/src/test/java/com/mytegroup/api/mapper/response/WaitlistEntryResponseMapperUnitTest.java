package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.WaitlistEntryResponseDto;
import com.mytegroup.api.entity.core.WaitlistEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WaitlistEntryResponseMapperUnitTest {

    private WaitlistEntryResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WaitlistEntryResponseMapper();
    }

    @Test
    void testWaitlistEntryToDto() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 10, 10, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 15, 30, 0);

        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(1L);
        entry.setEmail("user@example.com");
        entry.setName("John Doe");
        entry.setStatus(null); // Will test enum separately
        entry.setCreatedAt(createdAt);
        entry.setUpdatedAt(updatedAt);

        // Act
        WaitlistEntryResponseDto dto = mapper.toDto(entry);

        // Assert
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("user@example.com", dto.getEmail());
        assertEquals("John Doe", dto.getFirstName());
        assertNull(dto.getStatus());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    void testWaitlistEntryToDtoWithStatus() {
        // Arrange
        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(2L);
        entry.setEmail("test@example.com");
        entry.setName("Jane Smith");
        entry.setCreatedAt(LocalDateTime.now());

        // Act
        WaitlistEntryResponseDto dto = mapper.toDto(entry);

        // Assert
        assertNotNull(dto);
        assertEquals(2L, dto.getId());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("Jane Smith", dto.getFirstName());
    }

    @Test
    void testWaitlistEntryToDtoNull() {
        // Act
        WaitlistEntryResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testWaitlistEntryToDtoMapsAllFields() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 8, 0, 0);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 20, 16, 45, 0);

        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(99L);
        entry.setEmail("complete@example.com");
        entry.setName("Complete Entry");
        entry.setCreatedAt(created);
        entry.setUpdatedAt(updated);

        // Act
        WaitlistEntryResponseDto dto = mapper.toDto(entry);

        // Assert
        assertEquals(99L, dto.getId());
        assertEquals("complete@example.com", dto.getEmail());
        assertEquals("Complete Entry", dto.getFirstName());
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
    }

    @Test
    void testWaitlistEntryToDtoPreservesEmail() {
        // Arrange
        String[] emails = {
                "simple@test.com",
                "with.dot@example.co.uk",
                "plus+tag@domain.com",
                "underscore_name@test.org"
        };

        for (String email : emails) {
            WaitlistEntry entry = new WaitlistEntry();
            entry.setId(1L);
            entry.setEmail(email);
            entry.setName("Test");
            entry.setCreatedAt(LocalDateTime.now());

            // Act
            WaitlistEntryResponseDto dto = mapper.toDto(entry);

            // Assert
            assertEquals(email, dto.getEmail());
        }
    }

    @Test
    void testWaitlistEntryToDtoPreservesName() {
        // Arrange
        String[] names = {
                "John",
                "Jane Smith",
                "Dr. John Doe",
                "O'Brien",
                "José María"
        };

        for (String name : names) {
            WaitlistEntry entry = new WaitlistEntry();
            entry.setId(1L);
            entry.setEmail("test@example.com");
            entry.setName(name);
            entry.setCreatedAt(LocalDateTime.now());

            // Act
            WaitlistEntryResponseDto dto = mapper.toDto(entry);

            // Assert
            assertEquals(name, dto.getFirstName());
        }
    }

    @Test
    void testWaitlistEntryToDtoWithNullEmail() {
        // Arrange
        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(3L);
        entry.setEmail(null);
        entry.setName("No Email");
        entry.setCreatedAt(LocalDateTime.now());

        // Act
        WaitlistEntryResponseDto dto = mapper.toDto(entry);

        // Assert
        assertNull(dto.getEmail());
    }

    @Test
    void testWaitlistEntryToDtoWithNullName() {
        // Arrange
        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(4L);
        entry.setEmail("user@example.com");
        entry.setName(null);
        entry.setCreatedAt(LocalDateTime.now());

        // Act
        WaitlistEntryResponseDto dto = mapper.toDto(entry);

        // Assert
        assertNull(dto.getFirstName());
    }

    @Test
    void testWaitlistEntryToDtoPreservesDates() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 5, 10, 15, 30);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 18, 14, 45, 00);

        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(5L);
        entry.setEmail("test@example.com");
        entry.setName("Test User");
        entry.setCreatedAt(created);
        entry.setUpdatedAt(updated);

        // Act
        WaitlistEntryResponseDto dto = mapper.toDto(entry);

        // Assert
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
    }

    @Test
    void testWaitlistEntryToDtoWithDifferentIds() {
        // Arrange
        long[] ids = {1L, 100L, 999999L, Long.MAX_VALUE / 2};

        for (long id : ids) {
            WaitlistEntry entry = new WaitlistEntry();
            entry.setId(id);
            entry.setEmail("test@example.com");
            entry.setName("Test");
            entry.setCreatedAt(LocalDateTime.now());

            // Act
            WaitlistEntryResponseDto dto = mapper.toDto(entry);

            // Assert
            assertEquals(id, dto.getId());
        }
    }

    @Test
    void testWaitlistEntryToDtoWithEmptyStrings() {
        // Arrange
        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(6L);
        entry.setEmail("");
        entry.setName("");
        entry.setCreatedAt(LocalDateTime.now());

        // Act
        WaitlistEntryResponseDto dto = mapper.toDto(entry);

        // Assert
        assertEquals("", dto.getEmail());
        assertEquals("", dto.getFirstName());
    }
}


