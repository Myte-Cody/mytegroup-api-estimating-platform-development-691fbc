package com.mytegroup.api.controller.crmcontext;

import com.mytegroup.api.dto.crmcontext.ListCrmContextDocumentsQueryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm-context")
@PreAuthorize("isAuthenticated()")
public class CrmContextController {

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> listDocuments(@ModelAttribute ListCrmContextDocumentsQueryDto query) {
        return ResponseEntity.ok().build();
    }
}

