package com.mytegroup.api.controller.waitlist;

import com.mytegroup.api.dto.waitlist.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketing/waitlist")
public class WaitlistController {

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody @Valid StartWaitlistDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody @Valid VerifyWaitlistDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestBody @Valid ResendWaitlistDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestBody @Valid VerifyWaitlistPhoneDto dto) {
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@ModelAttribute ListWaitlistDto query) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invite-batch")
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> inviteBatch(@RequestBody @Valid InviteBatchDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/approve")
    @PreAuthorize("isAuthenticated() and hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> approve(@RequestBody @Valid ApproveWaitlistDto dto) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/event")
    public ResponseEntity<?> event(@RequestBody @Valid WaitlistEventDto dto) {
        return ResponseEntity.ok().build();
    }
}
