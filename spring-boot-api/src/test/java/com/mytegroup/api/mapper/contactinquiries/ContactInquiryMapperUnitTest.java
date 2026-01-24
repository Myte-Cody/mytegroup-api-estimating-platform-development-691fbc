package com.mytegroup.api.mapper.contactinquiries;

import com.mytegroup.api.dto.contactinquiries.CreateContactInquiryDto;
import com.mytegroup.api.entity.communication.ContactInquiry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class ContactInquiryMapperUnitTest {

    private ContactInquiryMapper contactInquiryMapper;

    @BeforeEach
    void setUp() {
        contactInquiryMapper = new ContactInquiryMapper();
    }

    @Test
    void testCreateContactInquiryDtoToEntity() {
        // Arrange
        CreateContactInquiryDto dto = new CreateContactInquiryDto(
            "John Doe",
            "john@example.com",
            "I have a question about pricing",
            "website"
        );

        // Act
        ContactInquiry inquiry = contactInquiryMapper.toEntity(dto);

        // Assert
        assertNotNull(inquiry);
        assertEquals("John Doe", inquiry.getName());
        assertEquals("john@example.com", inquiry.getEmail());
        assertEquals("I have a question about pricing", inquiry.getMessage());
        assertEquals("website", inquiry.getSource());
    }

    @Test
    void testCreateContactInquiryWithNullValues() {
        // Arrange
        CreateContactInquiryDto dto = new CreateContactInquiryDto(
            "Jane Smith",
            "jane@example.com",
            null,
            null
        );

        // Act
        ContactInquiry inquiry = contactInquiryMapper.toEntity(dto);

        // Assert
        assertNotNull(inquiry);
        assertEquals("Jane Smith", inquiry.getName());
        assertEquals("jane@example.com", inquiry.getEmail());
        assertNull(inquiry.getMessage());
        assertNull(inquiry.getSource());
    }
}

