package com.mytegroup.api.controller.contactinquiries;

import com.mytegroup.api.dto.contactinquiries.*;
import com.mytegroup.api.dto.response.ContactInquiryResponseDto;
import com.mytegroup.api.entity.communication.ContactInquiry;
import com.mytegroup.api.mapper.contactinquiries.ContactInquiryMapper;
import com.mytegroup.api.mapper.response.ContactInquiryResponseMapper;
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
    private final ContactInquiryMapper contactInquiryMapper;
    private final ContactInquiryResponseMapper contactInquiryResponseMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactInquiryResponseDto create(@RequestBody @Valid CreateContactInquiryDto dto) {
        ContactInquiry inquiry = contactInquiryMapper.toEntity(dto);
        
        ContactInquiry savedInquiry = contactInquiriesService.create(inquiry);
        
        return contactInquiryResponseMapper.toDto(savedInquiry);
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
    public List<ContactInquiryResponseDto> list(@ModelAttribute ListContactInquiriesDto query) {
        int page = query.getPage() != null ? query.getPage() : 0;
        int limit = query.getLimit() != null ? query.getLimit() : 25;
        
        Page<ContactInquiry> pageResult = contactInquiriesService.list(
            query.getStatus(),
            page,
            limit);
        
        return pageResult.getContent().stream()
            .map(contactInquiryResponseMapper::toDto)
            .toList();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ContactInquiryResponseDto update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateContactInquiryDto dto) {
        
        ContactInquiry inquiryUpdates = new ContactInquiry();
        inquiryUpdates.setStatus(dto.getStatus());
        // Note is not stored in ContactInquiry entity, it's only in the DTO
        
        ContactInquiry inquiry = contactInquiriesService.update(id, inquiryUpdates);
        
        return contactInquiryResponseMapper.toDto(inquiry);
    }
    
}
