package com.mytegroup.api.service.contactinquiries;

import com.mytegroup.api.entity.communication.ContactInquiry;
import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.communication.ContactInquiryRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactInquiriesServiceTest {

    @Mock
    private ContactInquiryRepository contactInquiryRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceValidationHelper validationHelper;

    @InjectMocks
    private ContactInquiriesService contactInquiriesService;

    private ContactInquiry testInquiry;

    @BeforeEach
    void setUp() {
        testInquiry = new ContactInquiry();
        testInquiry.setId(1L);
        testInquiry.setEmail("inquiry@example.com");
        testInquiry.setName("Test User");
        testInquiry.setStatus(ContactInquiryStatus.NEW);
    }

    @Test
    void testCreate_WithValidInquiry_CreatesInquiry() {
        ContactInquiry newInquiry = new ContactInquiry();
        newInquiry.setEmail("new@example.com");
        newInquiry.setName("New User");

        when(validationHelper.normalizeEmail("new@example.com")).thenReturn("new@example.com");
        when(contactInquiryRepository.save(any(ContactInquiry.class))).thenAnswer(invocation -> {
            ContactInquiry inquiry = invocation.getArgument(0);
            inquiry.setId(1L);
            return inquiry;
        });

        ContactInquiry result = contactInquiriesService.create(newInquiry);

        assertNotNull(result);
        assertEquals(ContactInquiryStatus.NEW, result.getStatus());
        verify(contactInquiryRepository, times(1)).save(any(ContactInquiry.class));
    }

    @Test
    void testCreate_WithInvalidEmail_ThrowsBadRequestException() {
        ContactInquiry newInquiry = new ContactInquiry();
        newInquiry.setEmail("invalid-email");

        when(validationHelper.normalizeEmail("invalid-email")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> {
            contactInquiriesService.create(newInquiry);
        });
    }

    @Test
    void testUpdate_WithValidUpdates_UpdatesInquiry() {
        Long inquiryId = 1L;
        ContactInquiry updates = new ContactInquiry();
        updates.setStatus(ContactInquiryStatus.CLOSED);
        updates.setRespondedAt(LocalDateTime.now());
        updates.setRespondedBy("admin");

        when(contactInquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));
        when(contactInquiryRepository.save(any(ContactInquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactInquiry result = contactInquiriesService.update(inquiryId, updates);

        assertNotNull(result);
        assertEquals(ContactInquiryStatus.CLOSED, result.getStatus());
        verify(contactInquiryRepository, times(1)).save(any(ContactInquiry.class));
    }

    @Test
    void testUpdate_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long inquiryId = 999L;
        ContactInquiry updates = new ContactInquiry();

        when(contactInquiryRepository.findById(inquiryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            contactInquiriesService.update(inquiryId, updates);
        });
    }

    @Test
    void testList_WithValidParams_ReturnsPage() {
        ContactInquiryStatus status = ContactInquiryStatus.NEW;
        Pageable pageable = PageRequest.of(0, 10);

        when(contactInquiryRepository.findByStatusOrderByCreatedAtDesc(eq(status), any(Pageable.class)))
            .thenReturn(Page.empty());

        Page<ContactInquiry> result = contactInquiriesService.list(status, 0, 10);

        assertNotNull(result);
        verify(contactInquiryRepository, times(1)).findByStatusOrderByCreatedAtDesc(eq(status), any(Pageable.class));
    }

    @Test
    void testList_WithNullStatus_ReturnsAllInquiries() {
        when(contactInquiryRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(Page.empty());

        Page<ContactInquiry> result = contactInquiriesService.list(null, 0, 10);

        assertNotNull(result);
        verify(contactInquiryRepository, times(1)).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }
}

