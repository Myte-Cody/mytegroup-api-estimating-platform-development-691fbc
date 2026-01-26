package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.SeatResponseDto;
import com.mytegroup.api.entity.projects.Seat;
import com.mytegroup.api.entity.core.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SeatResponseMapperUnitTest {

    private SeatResponseMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new SeatResponseMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Organization");
    }

    @Test
    void testSeatToDto() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 10, 10, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 15, 30, 0);

        Seat seat = new Seat();
        seat.setId(1L);
        seat.setRole("ADMIN");
        seat.setOrganization(organization);
        seat.setCreatedAt(createdAt);
        seat.setUpdatedAt(updatedAt);

        // Act
        SeatResponseDto dto = mapper.toDto(seat);

        // Assert
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("ADMIN", dto.getSeatType());
        assertEquals("1", dto.getOrgId());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    void testSeatToDtoWithNullOrganization() {
        // Arrange
        Seat seat = new Seat();
        seat.setId(2L);
        seat.setRole("USER");
        seat.setOrganization(null);
        seat.setCreatedAt(LocalDateTime.now());

        // Act
        SeatResponseDto dto = mapper.toDto(seat);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getOrgId());
        assertEquals("USER", dto.getSeatType());
    }

    @Test
    void testSeatToDtoNull() {
        // Act
        SeatResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testSeatToDtoMapsAllFields() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 8, 0, 0);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 20, 16, 45, 0);

        Seat seat = new Seat();
        seat.setId(99L);
        seat.setRole("VIEWER");
        seat.setOrganization(organization);
        seat.setCreatedAt(created);
        seat.setUpdatedAt(updated);

        // Act
        SeatResponseDto dto = mapper.toDto(seat);

        // Assert
        assertEquals(99L, dto.getId());
        assertEquals("VIEWER", dto.getSeatType());
        assertEquals("1", dto.getOrgId());
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
    }

    @Test
    void testSeatToDtoWithDifferentOrganizations() {
        // Arrange
        Organization org2 = new Organization();
        org2.setId(42L);

        Seat seat = new Seat();
        seat.setId(3L);
        seat.setRole("ADMIN");
        seat.setOrganization(org2);
        seat.setCreatedAt(LocalDateTime.now());

        // Act
        SeatResponseDto dto = mapper.toDto(seat);

        // Assert
        assertEquals("42", dto.getOrgId());
    }

    @Test
    void testSeatToDtoWithDifferentSeatTypes() {
        // Arrange
        String[] seatTypes = {"ADMIN", "USER", "VIEWER", "EDITOR", "GUEST"};

        for (String seatType : seatTypes) {
            Seat seat = new Seat();
            seat.setId(1L);
            seat.setRole(seatType);
            seat.setOrganization(organization);
            seat.setCreatedAt(LocalDateTime.now());

            // Act
            SeatResponseDto dto = mapper.toDto(seat);

            // Assert
            assertEquals(seatType, dto.getSeatType());
        }
    }

    @Test
    void testSeatToDtoPreservesTimestamps() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 5, 10, 15, 30);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 18, 14, 45, 00);

        Seat seat = new Seat();
        seat.setId(4L);
        seat.setRole("USER");
        seat.setOrganization(organization);
        seat.setCreatedAt(created);
        seat.setUpdatedAt(updated);

        // Act
        SeatResponseDto dto = mapper.toDto(seat);

        // Assert
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
    }

    @Test
    void testSeatToDtoWithDifferentIds() {
        // Arrange
        long[] ids = {1L, 100L, 999999L, Long.MAX_VALUE / 2};

        for (long id : ids) {
            Seat seat = new Seat();
            seat.setId(id);
            seat.setRole("USER");
            seat.setOrganization(organization);
            seat.setCreatedAt(LocalDateTime.now());

            // Act
            SeatResponseDto dto = mapper.toDto(seat);

            // Assert
            assertEquals(id, dto.getId());
        }
    }

    @Test
    void testSeatToDtoWithNullRole() {
        // Arrange
        Seat seat = new Seat();
        seat.setId(5L);
        seat.setRole(null);
        seat.setOrganization(organization);
        seat.setCreatedAt(LocalDateTime.now());

        // Act
        SeatResponseDto dto = mapper.toDto(seat);

        // Assert
        assertNull(dto.getSeatType());
    }

    @Test
    void testSeatToDtoWithEmptyRole() {
        // Arrange
        Seat seat = new Seat();
        seat.setId(6L);
        seat.setRole("");
        seat.setOrganization(organization);
        seat.setCreatedAt(LocalDateTime.now());

        // Act
        SeatResponseDto dto = mapper.toDto(seat);

        // Assert
        assertEquals("", dto.getSeatType());
    }

    @Test
    void testSeatToDtoWithSpecialCharactersInRole() {
        // Arrange
        Seat seat = new Seat();
        seat.setId(7L);
        seat.setRole("ROLE_WITH_SPECIAL_CHARS-123");
        seat.setOrganization(organization);
        seat.setCreatedAt(LocalDateTime.now());

        // Act
        SeatResponseDto dto = mapper.toDto(seat);

        // Assert
        assertEquals("ROLE_WITH_SPECIAL_CHARS-123", dto.getSeatType());
    }
}



