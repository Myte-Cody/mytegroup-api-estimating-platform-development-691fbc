package com.mytegroup.api.controller.ingestion;

import com.mytegroup.api.dto.ingestion.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingestion/contacts/v1")
@PreAuthorize("isAuthenticated()")
public class IngestionContactsController {

    @PostMapping("/suggest-mapping")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> suggestMapping(@RequestBody @Valid IngestionContactsSuggestMappingDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/parse-row")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> parseRow(@RequestBody @Valid IngestionContactsParseRowDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enrich")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> enrich(@RequestBody @Valid IngestionContactsEnrichDto dto) {
        return ResponseEntity.ok().build();
    }
}

