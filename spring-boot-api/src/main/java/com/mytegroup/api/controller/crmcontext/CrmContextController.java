package com.mytegroup.api.controller.crmcontext;

import com.mytegroup.api.dto.crmcontext.ListCrmContextDocumentsQueryDto;
import com.mytegroup.api.service.crmcontext.CrmContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crm-context")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CrmContextController {

    private final CrmContextService crmContextService;

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> listDocuments(
            @ModelAttribute ListCrmContextDocumentsQueryDto query,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Map<String, Object> result = crmContextService.listDocuments(
            orgId,
            query.getEntityType(),
            query.getEntityId(),
            query.getPage() != null ? query.getPage() : 0,
            query.getLimit() != null ? query.getLimit() : 25
        );
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/documents/{documentId}/index")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> indexDocument(
            @PathVariable String documentId,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        // TODO: Implement indexDocument method - need entityType, entityId, title, text, metadata
        throw new UnsupportedOperationException("indexDocument not yet fully implemented");
    }
}
