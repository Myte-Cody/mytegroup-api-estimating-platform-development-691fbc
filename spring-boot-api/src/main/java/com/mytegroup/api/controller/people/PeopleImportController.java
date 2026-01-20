package com.mytegroup.api.controller.people;

import com.mytegroup.api.dto.people.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/people/import")
@PreAuthorize("isAuthenticated()")
public class PeopleImportController {

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> preview(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> confirm(@RequestBody @Valid PeopleImportConfirmDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/preview")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> previewV1(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/confirm")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> confirmV1(@RequestBody @Valid PeopleImportV1ConfirmDto dto) {
        return ResponseEntity.ok().build();
    }
}

