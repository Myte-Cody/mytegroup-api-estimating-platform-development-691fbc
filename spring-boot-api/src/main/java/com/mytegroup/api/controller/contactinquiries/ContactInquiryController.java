package com.mytegroup.api.controller.contactinquiries;

import com.mytegroup.api.dto.contactinquiries.*;
import com.mytegroup.api.entity.communication.ContactInquiry;
import com.mytegroup.api.service.contactinquiries.ContactInquiriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing/contact-inquiries")
@RequiredArgsConstructor
public class ContactInquiryController {

    private final ContactInquiriesService contactInquiriesService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody @Valid CreateContactInquiryDto dto) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setName(dto.getName());
        inquiry.setEmail(dto.getEmail());
        inquiry.setMessage(dto.getMessage());
        inquiry.setSource(dto.getSource());
        
        ContactInquiry savedInquiry = contactInquiriesService.create(inquiry);
        
        return inquiryToResponse(savedInquiry);
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody @Valid VerifyContactInquiryDto dto) {
        // TODO: Implement verify method in service
        throw new UnsupportedOperationException("Verify not yet implemented");
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody @Valid ConfirmContactInquiryDto dto) {
        // TODO: Implement confirm method in service
        throw new UnsupportedOperationException("Confirm not yet implemented");
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() && hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<Map<String, Object>> list(@ModelAttribute ListContactInquiriesDto query) {
        int page = query.getPage() != null ? query.getPage() : 0;
        int limit = query.getLimit() != null ? query.getLimit() : 25;
        
        Page<ContactInquiry> pageResult = contactInquiriesService.list(
            query.getStatus(),
            page,
            limit);
        
        return pageResult.getContent().stream()
            .map(this::inquiryToResponse)
            .toList();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateContactInquiryDto dto) {
        
        ContactInquiry inquiryUpdates = new ContactInquiry();
        inquiryUpdates.setStatus(dto.getStatus());
        // Note is not stored in ContactInquiry entity, it's only in the DTO
        
        ContactInquiry inquiry = contactInquiriesService.update(id, inquiryUpdates);
        
        return inquiryToResponse(inquiry);
    }
    
    private Map<String, Object> inquiryToResponse(ContactInquiry inquiry) {
        return Map.of(
            "id", inquiry.getId(),
            "name", inquiry.getName() != null ? inquiry.getName() : "",
            "email", inquiry.getEmail() != null ? inquiry.getEmail() : "",
            "message", inquiry.getMessage() != null ? inquiry.getMessage() : "",
            "status", inquiry.getStatus() != null ? inquiry.getStatus().name() : "",
            "source", inquiry.getSource() != null ? inquiry.getSource() : "",
            "createdAt", inquiry.getCreatedAt() != null ? inquiry.getCreatedAt().toString() : ""
        );
    }
    
}
