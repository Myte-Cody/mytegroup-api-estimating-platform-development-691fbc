package com.mytegroup.api.controller.compliance;

import com.mytegroup.api.dto.compliance.*;
import com.mytegroup.api.service.compliance.ComplianceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    @PostMapping("/strip-pii")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> stripPii(@RequestBody @Valid StripPiiDto dto) {
        complianceService.stripPii(
            dto.getEntityType(),
            Long.parseLong(dto.getEntityId()),
            dto.getOrgId());
        Map<String, Object> result = Map.of("status", "ok");
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/legal-hold")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> setLegalHold(@RequestBody @Valid SetLegalHoldDto dto) {
        complianceService.setLegalHold(
            dto.getEntityType(),
            Long.parseLong(dto.getEntityId()),
            dto.getLegalHold(),
            dto.getOrgId());
        Map<String, Object> result = Map.of("status", "ok");
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch-archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> batchArchive(@RequestBody @Valid BatchArchiveDto dto) {
        List<Long> entityIds = dto.getEntityIds().stream()
            .map(Long::parseLong)
            .toList();
        Map<String, Integer> result = complianceService.batchArchive(
            dto.getEntityType(),
            entityIds,
            dto.getOrgId());
        
        return ResponseEntity.ok(result);
    }
}
