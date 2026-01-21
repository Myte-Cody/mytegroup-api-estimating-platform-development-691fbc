package com.mytegroup.api.controller.persons;

import com.mytegroup.api.dto.persons.*;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.persons.PersonsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
@RequiredArgsConstructor
public class PersonController {

    private final PersonsService personsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String personType,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Page<Person> persons = personsService.list(actor, resolvedOrgId, search, personType, includeArchived, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", persons.getContent().stream().map(this::personToMap).toList());
        response.put("total", persons.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @RequestBody @Valid CreatePersonDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Person person = new Person();
        person.setFirstName(dto.getFirstName());
        person.setLastName(dto.getLastName());
        person.setPrimaryEmail(dto.getPrimaryEmail());
        person.setPrimaryPhone(dto.getPrimaryPhone());
        person.setJobTitle(dto.getJobTitle());
        person.setNotes(dto.getNotes());
        person.setExternalId(dto.getExternalId());
        
        Person savedPerson = personsService.create(person, actor, resolvedOrgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(personToMap(savedPerson));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Person person = personsService.getById(id, actor, resolvedOrgId, includeArchived);
        
        return ResponseEntity.ok(personToMap(person));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePersonDto dto,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Person personUpdates = new Person();
        personUpdates.setFirstName(dto.getFirstName());
        personUpdates.setLastName(dto.getLastName());
        personUpdates.setPrimaryEmail(dto.getPrimaryEmail());
        personUpdates.setPrimaryPhone(dto.getPrimaryPhone());
        personUpdates.setJobTitle(dto.getJobTitle());
        personUpdates.setNotes(dto.getNotes());
        personUpdates.setExternalId(dto.getExternalId());
        
        Person updatedPerson = personsService.update(id, personUpdates, actor, resolvedOrgId);
        
        return ResponseEntity.ok(personToMap(updatedPerson));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Person archivedPerson = personsService.archive(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(personToMap(archivedPerson));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Person unarchivedPerson = personsService.unarchive(id, actor, resolvedOrgId);
        
        return ResponseEntity.ok(personToMap(unarchivedPerson));
    }
    
    // Helper methods
    
    private Map<String, Object> personToMap(Person person) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", person.getId());
        map.put("firstName", person.getFirstName());
        map.put("lastName", person.getLastName());
        map.put("primaryEmail", person.getPrimaryEmail());
        map.put("primaryPhone", person.getPrimaryPhone());
        map.put("jobTitle", person.getJobTitle());
        map.put("personType", person.getPersonType() != null ? person.getPersonType().getValue() : null);
        map.put("notes", person.getNotes());
        map.put("externalId", person.getExternalId());
        map.put("userId", person.getUser() != null ? person.getUser().getId() : null);
        map.put("companyId", person.getCompany() != null ? person.getCompany().getId() : null);
        map.put("piiStripped", person.getPiiStripped());
        map.put("legalHold", person.getLegalHold());
        map.put("archivedAt", person.getArchivedAt());
        map.put("orgId", person.getOrganization() != null ? person.getOrganization().getId() : null);
        map.put("createdAt", person.getCreatedAt());
        map.put("updatedAt", person.getUpdatedAt());
        return map;
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
