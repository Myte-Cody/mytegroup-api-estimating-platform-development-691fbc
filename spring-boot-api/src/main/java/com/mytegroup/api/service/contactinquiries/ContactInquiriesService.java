package com.mytegroup.api.service.contactinquiries;

import com.mytegroup.api.entity.communication.ContactInquiry;
import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.communication.ContactInquiryRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceValidationHelper;
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
 * Service for contact inquiry management.
 * Handles contact form submissions, verification, and confirmation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactInquiriesService {
    
    private final ContactInquiryRepository contactInquiryRepository;
    private final AuditLogService auditLogService;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Creates a new contact inquiry
     */
    @Transactional
    public ContactInquiry create(ContactInquiry inquiry, ActorContext actor) {
        String email = validationHelper.normalizeEmail(inquiry.getEmail());
        if (email == null || email.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        inquiry.setEmail(email);
        inquiry.setStatus(ContactInquiryStatus.PENDING);
        
        ContactInquiry savedInquiry = contactInquiryRepository.save(inquiry);
        
        auditLogService.log(
            "contact_inquiry.created",
            null,
            null,
            "ContactInquiry",
            savedInquiry.getId().toString(),
            Map.of("email", email)
        );
        
        // TODO: Send verification email
        
        return savedInquiry;
    }
    
    /**
     * Updates a contact inquiry
     */
    @Transactional
    public ContactInquiry update(Long id, ContactInquiry inquiryUpdates) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contact inquiry not found"));
        
        if (inquiryUpdates.getStatus() != null) {
            inquiry.setStatus(inquiryUpdates.getStatus());
        }
        if (inquiryUpdates.getRespondedAt() != null) {
            inquiry.setRespondedAt(inquiryUpdates.getRespondedAt());
        }
        if (inquiryUpdates.getRespondedBy() != null) {
            inquiry.setRespondedBy(inquiryUpdates.getRespondedBy());
        }
        
        ContactInquiry savedInquiry = contactInquiryRepository.save(inquiry);
        
        auditLogService.log(
            "contact_inquiry.updated",
            null,
            null,
            "ContactInquiry",
            savedInquiry.getId().toString(),
            Map.of("status", savedInquiry.getStatus().toString())
        );
        
        return savedInquiry;
    }
    
    /**
     * Lists contact inquiries
     */
    @Transactional(readOnly = true)
    public Page<ContactInquiry> list(ContactInquiryStatus status, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        
        if (status != null) {
            return contactInquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return contactInquiryRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}

