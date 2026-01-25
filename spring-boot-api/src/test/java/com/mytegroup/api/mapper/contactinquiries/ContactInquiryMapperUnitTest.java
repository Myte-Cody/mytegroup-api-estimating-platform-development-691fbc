package com.mytegroup.api.mapper.contactinquiries;

import com.mytegroup.api.dto.contactinquiries.CreateContactInquiryDto;
import com.mytegroup.api.dto.response.ContactInquiryResponseDto;
import com.mytegroup.api.entity.communication.ContactInquiry;
import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ContactInquiryMapperUnitTest {

    private ContactInquiryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ContactInquiryMapper();
    }

    @Test
    void testToEntityWithAllFields() {
        // Arrange
        CreateContactInquiryDto dto = new CreateContactInquiryDto();
        dto.setName("John Doe");
        dto.setEmail("john@example.com");
        dto.setMessage("I have a question about your service");
        dto.setSource("WEB_FORM");

        // Act
        ContactInquiry entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals("John Doe", entity.getName());
        assertEquals("john@example.com", entity.getEmail());
        assertEquals("I have a question about your service", entity.getMessage());
        assertEquals("WEB_FORM", entity.getSource());
    }

    @Test
    void testToEntityWithMinimalFields() {
        // Arrange
        CreateContactInquiryDto dto = new CreateContactInquiryDto();
        dto.setName("Jane");
        dto.setEmail("jane@example.com");
        dto.setMessage("Hello");
        dto.setSource(null);

        // Act
        ContactInquiry entity = mapper.toEntity(dto);

        // Assert
        assertEquals("Jane", entity.getName());
        assertEquals("jane@example.com", entity.getEmail());
        assertEquals("Hello", entity.getMessage());
        assertNull(entity.getSource());
    }

    @Test
    void testToEntityWithNullValues() {
        // Arrange
        CreateContactInquiryDto dto = new CreateContactInquiryDto();
        dto.setName(null);
        dto.setEmail(null);
        dto.setMessage(null);
        dto.setSource(null);

        // Act
        ContactInquiry entity = mapper.toEntity(dto);

        // Assert
        assertNull(entity.getName());
        assertNull(entity.getEmail());
        assertNull(entity.getMessage());
        assertNull(entity.getSource());
    }

    @Test
    void testToEntityMapsAllFields() {
        // Arrange
        CreateContactInquiryDto dto = new CreateContactInquiryDto();
        dto.setName("Complete Name");
        dto.setEmail("complete@example.com");
        dto.setMessage("Complete message with full details");
        dto.setSource("EMAIL");

        // Act
        ContactInquiry entity = mapper.toEntity(dto);

        // Assert
        assertEquals("Complete Name", entity.getName());
        assertEquals("complete@example.com", entity.getEmail());
        assertEquals("Complete message with full details", entity.getMessage());
        assertEquals("EMAIL", entity.getSource());
    }

    @Test
    void testToDtoWithFullEntity() {
        // Arrange
        ContactInquiry entity = new ContactInquiry();
        entity.setId(10L);
        entity.setName("Test Name");
        entity.setEmail("test@example.com");
        entity.setStatus(ContactInquiryStatus.NEW);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        ContactInquiryResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Test Name", dto.getFirstName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("new", dto.getStatus());
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 1, 11, 0, 0), dto.getUpdatedAt());
    }

    @Test
    void testToDtoWithNullEntity() {
        // Act
        ContactInquiryResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDtoWithNullStatus() {
        // Arrange
        ContactInquiry entity = new ContactInquiry();
        entity.setId(11L);
        entity.setName("No Status");
        entity.setEmail("nostatus@example.com");
        entity.setStatus(null);

        // Act
        ContactInquiryResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getStatus());
        assertEquals("No Status", dto.getFirstName());
    }

    @Test
    void testToDoDifferentStatuses() {
        // Arrange
        ContactInquiryStatus[] statuses = {
                ContactInquiryStatus.NEW,
                ContactInquiryStatus.IN_PROGRESS,
                ContactInquiryStatus.CLOSED
        };

        for (ContactInquiryStatus status : statuses) {
            ContactInquiry entity = new ContactInquiry();
            entity.setId(12L);
            entity.setName("Test");
            entity.setEmail("test@example.com");
            entity.setStatus(status);

            // Act
            ContactInquiryResponseDto dto = mapper.toDto(entity);

            // Assert
            assertEquals(status.getValue(), dto.getStatus());
        }
    }

    @Test
    void testToDtoMapsAllFields() {
        // Arrange
        ContactInquiry entity = new ContactInquiry();
        entity.setId(13L);
        entity.setName("Complete Inquiry");
        entity.setEmail("complete@example.com");
        entity.setStatus(ContactInquiryStatus.IN_PROGRESS);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 30, 0));

        // Act
        ContactInquiryResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("Complete Inquiry", dto.getFirstName());
        assertEquals("complete@example.com", dto.getEmail());
        assertEquals("in-progress", dto.getStatus());
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), dto.getCreatedAt());
    }

    @Test
    void testToDtoWithNullDates() {
        // Arrange
        ContactInquiry entity = new ContactInquiry();
        entity.setId(14L);
        entity.setName("Inquiry");
        entity.setEmail("test@example.com");
        entity.setStatus(ContactInquiryStatus.NEW);
        entity.setCreatedAt(null);
        entity.setUpdatedAt(null);

        // Act
        ContactInquiryResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getCreatedAt());
        assertNull(dto.getUpdatedAt());
    }
}

