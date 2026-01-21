package com.mytegroup.api.controller.crmcontext;

import com.mytegroup.api.dto.crmcontext.ListCrmContextDocumentsQueryDto;
import com.mytegroup.api.service.common.ActorContext;
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
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        List<Map<String, Object>> documents = crmContextService.listDocuments(
            query.getEntityType(),
            query.getEntityId(),
            query.getPage(),
            query.getLimit(),
            actor,
            resolvedOrgId
        );
        
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/documents/{documentId}/index")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> indexDocument(
            @PathVariable String documentId,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Map<String, Object> result = crmContextService.indexDocument(documentId, actor, resolvedOrgId);
        
        return ResponseEntity.ok(result);
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
