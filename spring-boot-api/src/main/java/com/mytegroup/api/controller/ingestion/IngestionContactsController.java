package com.mytegroup.api.controller.ingestion;

import com.mytegroup.api.dto.ingestion.*;
import com.mytegroup.api.service.ingestion.IngestionContactsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ingestion/contacts/v1")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class IngestionContactsController {

    private final IngestionContactsService ingestionContactsService;

    @PostMapping("/suggest-mapping")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> suggestMapping(
            @RequestBody @Valid IngestionContactsSuggestMappingDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Map<String, Object> mapping = ingestionContactsService.suggestMapping(
            dto.getHeaders(),
            dto.getSampleRows(),
            orgId
        );
        
        return ResponseEntity.ok(mapping);
    }

    @PostMapping("/parse-row")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> parseRow(
            @RequestBody @Valid IngestionContactsParseRowDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Map<String, Object> result = ingestionContactsService.parseRow(
            dto.getRow(),
            dto.getMapping(),
            orgId
        );
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/enrich")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> enrich(
            @RequestBody @Valid IngestionContactsEnrichDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Map<String, Object> result = ingestionContactsService.enrich(
            dto.getContact(),
            orgId
        );
        
        return ResponseEntity.ok(result);
    }
    
        }
        
