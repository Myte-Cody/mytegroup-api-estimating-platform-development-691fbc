package com.mytegroup.api.controller.waitlist;

import com.mytegroup.api.dto.waitlist.*;
import com.mytegroup.api.entity.core.WaitlistEntry;
import com.mytegroup.api.entity.enums.core.WaitlistStatus;
import com.mytegroup.api.entity.enums.core.WaitlistVerifyStatus;
import com.mytegroup.api.service.waitlist.WaitlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Waitlist controller.
 * Endpoints:
 * - POST /marketing/waitlist/start - Start waitlist entry (Public)
 * - POST /marketing/waitlist/verify - Verify email (Public)
 * - POST /marketing/waitlist/verify-phone - Verify phone (Public)
 * - POST /marketing/waitlist/resend - Resend verification (Public)
 * - POST /marketing/waitlist/resend-phone - Resend phone verification (Public)
 * - GET /marketing/waitlist - List waitlist entries (Admin+)
 * - GET /marketing/waitlist/stats - Get waitlist stats (Public)
 * - POST /marketing/waitlist/invite-batch - Process invite batch (Admin+)
 * - POST /marketing/waitlist/approve - Approve and invite (Admin+)
 * - POST /marketing/waitlist/event - Log marketing event (Public)
 */
@RestController
@RequestMapping("/api/marketing/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody @Valid StartWaitlistDto dto, HttpServletRequest request) {
        String ip = getClientIp(request);
        
        Map<String, Object> result = waitlistService.start(
            dto.getEmail(),
            dto.getPhone(),
            dto.getName(),
            dto.getRole() != null ? dto.getRole().getValue() : null,
            dto.getSource(),
            Boolean.TRUE.equals(dto.getPreCreateAccount()),
            Boolean.TRUE.equals(dto.getMarketingConsent())
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", result.get("status"));
        
        WaitlistEntry entry = (WaitlistEntry) result.get("entry");
        if (entry != null) {
            response.put("entry", entryToMap(entry));
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody @Valid VerifyWaitlistDto dto) {
        WaitlistEntry entry = waitlistService.verifyEmail(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(entryToMap(entry));
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestBody @Valid VerifyWaitlistPhoneDto dto) {
        WaitlistEntry entry = waitlistService.verifyPhone(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(entryToMap(entry));
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestBody @Valid ResendWaitlistDto dto, HttpServletRequest request) {
        String ip = getClientIp(request);
        Map<String, String> result = waitlistService.resendEmail(dto.getEmail());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/resend-phone")
    public ResponseEntity<?> resendPhone(@RequestBody @Valid ResendWaitlistDto dto, HttpServletRequest request) {
        // TODO: Implement phone resend when SMS service is complete
        return ResponseEntity.ok(Map.of("status", "not_implemented"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String verifyStatus,
            @RequestParam(required = false) String cohortTag,
            @RequestParam(required = false) String emailContains,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        WaitlistStatus statusEnum = status != null ? WaitlistStatus.valueOf(status.toUpperCase()) : null;
        WaitlistVerifyStatus verifyStatusEnum = verifyStatus != null 
            ? WaitlistVerifyStatus.valueOf(verifyStatus.toUpperCase()) 
            : null;
        
        Page<WaitlistEntry> entries = waitlistService.list(statusEnum, verifyStatusEnum, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", entries.getContent().stream().map(this::entryToMap).toList());
        response.put("total", entries.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        Map<String, Object> stats = waitlistService.stats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/invite-batch")
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> inviteBatch(@RequestBody @Valid InviteBatchDto dto) {
        // TODO: Implement batch invite processing
        return ResponseEntity.ok(Map.of(
            "invited", 0,
            "skipped", false,
            "message", "Batch invite processing not yet implemented"
        ));
    }

    @PostMapping("/approve")
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> approve(@RequestBody @Valid ApproveWaitlistDto dto) {
        WaitlistEntry entry = waitlistService.markInvited(dto.getEmail(), dto.getCohortTag());
        
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(entryToMap(entry));
    }

    @PostMapping("/event")
    public ResponseEntity<?> event(@RequestBody @Valid WaitlistEventDto dto, HttpServletRequest request) {
        String ip = getClientIp(request);
        
        try {
            waitlistService.logEvent(dto.getEvent(), dto.getMeta());
        } catch (Exception e) {
            // Rate limited or other error - don't expose
        }
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    // Helper methods
    
    private Map<String, Object> entryToMap(WaitlistEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entry.getId());
        map.put("email", entry.getEmail());
        map.put("name", entry.getName());
        map.put("phone", entry.getPhone());
        map.put("role", entry.getRole());
        map.put("source", entry.getSource());
        map.put("status", entry.getStatus() != null ? entry.getStatus().name().toLowerCase() : null);
        map.put("verifyStatus", entry.getVerifyStatus() != null ? entry.getVerifyStatus().name().toLowerCase() : null);
        map.put("phoneVerifyStatus", entry.getPhoneVerifyStatus() != null ? entry.getPhoneVerifyStatus().name().toLowerCase() : null);
        map.put("verifiedAt", entry.getVerifiedAt());
        map.put("phoneVerifiedAt", entry.getPhoneVerifiedAt());
        map.put("invitedAt", entry.getInvitedAt());
        map.put("activatedAt", entry.getActivatedAt());
        map.put("cohortTag", entry.getCohortTag());
        map.put("preCreateAccount", entry.getPreCreateAccount());
        map.put("marketingConsent", entry.getMarketingConsent());
        map.put("createdAt", entry.getCreatedAt());
        return map;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
