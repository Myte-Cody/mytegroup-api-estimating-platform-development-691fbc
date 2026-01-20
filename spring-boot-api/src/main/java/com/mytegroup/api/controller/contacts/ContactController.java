package com.mytegroup.api.controller.contacts;

import com.mytegroup.api.dto.contacts.ListContactsQueryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
@PreAuthorize("isAuthenticated()")
public class ContactController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@ModelAttribute ListContactsQueryDto query) {
        return ResponseEntity.ok().build();
    }
}
