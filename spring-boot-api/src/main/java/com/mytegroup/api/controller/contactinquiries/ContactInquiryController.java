package com.mytegroup.api.controller.contactinquiries;

import com.mytegroup.api.dto.contactinquiries.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketing/contact-inquiries")
public class ContactInquiryController {

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CreateContactInquiryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody @Valid VerifyContactInquiryDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody @Valid ConfirmContactInquiryDto dto) {
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() && hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@ModelAttribute ListContactInquiriesDto query) {
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody @Valid UpdateContactInquiryDto dto) {
        return ResponseEntity.ok().build();
    }
}

