package com.mytegroup.api.controller.emailtemplates;

import com.mytegroup.api.dto.emailtemplates.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-templates")
@PreAuthorize("isAuthenticated()")
public class EmailTemplateController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> get(@PathVariable String name) {
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(@PathVariable String name, @RequestBody @Valid UpdateEmailTemplateDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{name}/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(@PathVariable String name, @RequestBody @Valid PreviewEmailTemplateDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{name}/test-send")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> testSend(@PathVariable String name, @RequestBody @Valid TestSendTemplateDto dto) {
        return ResponseEntity.ok().build();
    }
}

