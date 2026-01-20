package com.mytegroup.api.controller.persons;

import com.mytegroup.api.dto.persons.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Persons controller.
 * Endpoints:
 * - GET /persons - List persons (Admin+)
 * - POST /persons - Create person (Admin+)
 * - GET /persons/:id - Get person (Admin+)
 * - PATCH /persons/:id - Update person (Admin+)
 * - POST /persons/:id/archive - Archive person (Admin+)
 * - POST /persons/:id/unarchive - Unarchive person (Admin+)
 */
@RestController
@RequestMapping("/api/persons")
@PreAuthorize("isAuthenticated()")
public class PersonController {

    // TODO: Inject PersonService, PersonMapper

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(@ModelAttribute ListPersonsQueryDto query) {
        // TODO: Implement list persons logic
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(@RequestBody @Valid CreatePersonDto dto) {
        // TODO: Implement create person logic
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(@PathVariable String id) {
        // TODO: Implement get person by id logic
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody @Valid UpdatePersonDto dto) {
        // TODO: Implement update person logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(@PathVariable String id) {
        // TODO: Implement archive person logic
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(@PathVariable String id) {
        // TODO: Implement unarchive person logic
        return ResponseEntity.ok().build();
    }
}

