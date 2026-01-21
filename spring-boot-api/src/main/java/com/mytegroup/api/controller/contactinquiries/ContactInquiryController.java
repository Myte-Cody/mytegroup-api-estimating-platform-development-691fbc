package com.mytegroup.api.controller.contactinquiries;

import com.mytegroup.api.dto.contactinquiries.*;
import com.mytegroup.api.entity.core.ContactInquiry;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.contactinquiries.ContactInquiriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
        ContactInquiry inquiry = contactInquiriesService.create(
            dto.getName(),
            dto.getEmail(),
            dto.getPhone(),
            dto.getCompanyName(),
            dto.getMessage(),
            dto.getSource(),
            dto.getTrap()
        );
        
        return inquiryToResponse(inquiry);
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody @Valid VerifyContactInquiryDto dto) {
        ContactInquiry inquiry = contactInquiriesService.verify(dto.getEmail(), dto.getCode());
        
        return inquiryToResponse(inquiry);
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody @Valid ConfirmContactInquiryDto dto) {
        ContactInquiry inquiry = contactInquiriesService.confirm(dto.getEmail());
        
        return inquiryToResponse(inquiry);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() && hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<Map<String, Object>> list(@ModelAttribute ListContactInquiriesDto query) {
        ActorContext actor = getActorContext();
        
        List<ContactInquiry> inquiries = contactInquiriesService.list(
            query.getStatus(),
            query.getEmailContains(),
            query.getPage(),
            query.getLimit(),
            actor
        );
        
        return inquiries.stream()
            .map(this::inquiryToResponse)
            .toList();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateContactInquiryDto dto) {
        
        ActorContext actor = getActorContext();
        
        ContactInquiry inquiry = contactInquiriesService.update(id, dto.getStatus(), dto.getNote(), actor);
        
        return inquiryToResponse(inquiry);
    }
    
    private Map<String, Object> inquiryToResponse(ContactInquiry inquiry) {
        return Map.of(
            "id", inquiry.getId(),
            "name", inquiry.getName() != null ? inquiry.getName() : "",
            "email", inquiry.getEmail() != null ? inquiry.getEmail() : "",
            "phone", inquiry.getPhone() != null ? inquiry.getPhone() : "",
            "companyName", inquiry.getCompanyName() != null ? inquiry.getCompanyName() : "",
            "message", inquiry.getMessage() != null ? inquiry.getMessage() : "",
            "status", inquiry.getStatus() != null ? inquiry.getStatus().name() : "",
            "source", inquiry.getSource() != null ? inquiry.getSource() : "",
            "createdAt", inquiry.getCreatedAt() != null ? inquiry.getCreatedAt().toString() : ""
        );
    }
    
    private ActorContext getActorContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new ActorContext(null, null, null, null);
        }
        
        Long userId = null;
        if (auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        } else if (auth.getPrincipal() instanceof String) {
            try {
                userId = Long.parseLong((String) auth.getPrincipal());
            } catch (NumberFormatException ignored) {}
        }
        
        return new ActorContext(
            userId != null ? userId.toString() : null,
            null,
            null,
            null
        );
    }
}
