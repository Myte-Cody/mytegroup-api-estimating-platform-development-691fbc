package com.mytegroup.api.controller.legal;

import com.mytegroup.api.dto.legal.*;
import com.mytegroup.api.entity.legal.LegalDoc;
import com.mytegroup.api.entity.enums.legal.LegalDocType;
import com.mytegroup.api.mapper.legal.LegalMapper;
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
    private final LegalMapper legalMapper;

    @GetMapping("/docs")
    public List<Map<String, Object>> listDocs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean currentOnly) {
        
        List<LegalDoc> docs = legalService.list(type);
        
        // Filter by currentOnly if requested
        if (currentOnly != null && currentOnly) {
            docs = docs.stream()
                .filter(doc -> doc.getArchivedAt() == null)
                .toList();
        }
        
        return docs.stream()
            .map(this::docToResponse)
            .toList();
    }

    @GetMapping("/docs/{id}")
    public Map<String, Object> getDoc(@PathVariable Long id) {
        // TODO: Implement getById method in service
        throw new UnsupportedOperationException("getById not yet implemented");
    }

    @PostMapping("/docs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Map<String, Object> createDoc(@RequestBody @Valid CreateLegalDocDto dto) {
        LegalDoc doc = legalMapper.toEntity(dto);
        LegalDoc savedDoc = legalService.createDoc(doc);
        
        return docToResponse(savedDoc);
    }

    @PostMapping("/accept")
    public Map<String, Object> accept(@RequestBody @Valid AcceptLegalDocDto dto) {
        // TODO: Need to get type and version from docId
        // For now, throw unsupported
        throw new UnsupportedOperationException("Accept not yet fully implemented - need doc type and version");
    }

    @GetMapping("/acceptance-status")
    public Map<String, Object> acceptanceStatus(
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String orgId) {
        // TODO: Implement getAcceptanceStatus method in service
        throw new UnsupportedOperationException("getAcceptanceStatus not yet implemented");
    }
    
    private Map<String, Object> docToResponse(LegalDoc doc) {
        return Map.of(
            "id", doc.getId(),
            "type", doc.getType() != null ? doc.getType().name() : "",
            "version", doc.getVersion() != null ? doc.getVersion() : "",
            "content", doc.getContent() != null ? doc.getContent() : "",
            "effectiveAt", doc.getEffectiveAt() != null ? doc.getEffectiveAt().toString() : "",
            "isCurrent", doc.getArchivedAt() == null,
            "createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : ""
        );
    }
    
}
