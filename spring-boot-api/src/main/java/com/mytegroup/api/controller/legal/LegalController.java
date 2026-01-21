package com.mytegroup.api.controller.legal;

import com.mytegroup.api.dto.legal.*;
import com.mytegroup.api.entity.core.LegalDoc;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.legal.LegalService;
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
@RequestMapping("/api/legal")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class LegalController {

    private final LegalService legalService;

    @GetMapping("/docs")
    public List<Map<String, Object>> listDocs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean currentOnly) {
        
        ActorContext actor = getActorContext();
        
        List<LegalDoc> docs = legalService.list(type, currentOnly != null && currentOnly, actor);
        
        return docs.stream()
            .map(this::docToResponse)
            .toList();
    }

    @GetMapping("/docs/{id}")
    public Map<String, Object> getDoc(@PathVariable Long id) {
        ActorContext actor = getActorContext();
        
        LegalDoc doc = legalService.getById(id, actor);
        
        return docToResponse(doc);
    }

    @PostMapping("/docs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Map<String, Object> createDoc(@RequestBody @Valid CreateLegalDocDto dto) {
        ActorContext actor = getActorContext();
        
        LegalDoc doc = legalService.create(
            dto.getType(),
            dto.getVersion(),
            dto.getTitle(),
            dto.getContent(),
            dto.getEffectiveAt(),
            actor
        );
        
        return docToResponse(doc);
    }

    @PostMapping("/accept")
    public Map<String, Object> accept(@RequestBody @Valid AcceptLegalDocDto dto) {
        ActorContext actor = getActorContext();
        
        return legalService.accept(dto.getDocId(), dto.getOrgId(), actor);
    }

    @GetMapping("/acceptance-status")
    public Map<String, Object> acceptanceStatus(
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        
        return legalService.getAcceptanceStatus(docType, orgId, actor);
    }
    
    private Map<String, Object> docToResponse(LegalDoc doc) {
        return Map.of(
            "id", doc.getId(),
            "type", doc.getType() != null ? doc.getType() : "",
            "version", doc.getVersion() != null ? doc.getVersion() : "",
            "title", doc.getTitle() != null ? doc.getTitle() : "",
            "content", doc.getContent() != null ? doc.getContent() : "",
            "effectiveAt", doc.getEffectiveAt() != null ? doc.getEffectiveAt().toString() : "",
            "isCurrent", doc.isCurrent(),
            "createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : ""
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
