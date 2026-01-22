package com.mytegroup.api.controller.persons;

import com.mytegroup.api.dto.persons.*;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.service.persons.PersonsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
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
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        List<Person> persons = personsService.list(orgId, includeArchived != null && includeArchived);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", persons.stream().map(this::personToMap).toList());
        response.put("total", persons.size());
        response.put("page", page);
        response.put("limit", limit);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> create(
            @RequestBody @Valid CreatePersonDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Person person = new Person();
        person.setDisplayName(dto.displayName());
        person.setFirstName(dto.firstName());
        person.setLastName(dto.lastName());
        person.setPrimaryEmail(dto.primaryEmail());
        person.setPrimaryPhoneE164(dto.primaryPhone());
        person.setPersonType(dto.personType());
        person.setDateOfBirth(dto.dateOfBirth());
        // Emails and phones are complex types - will be set by service if needed
        // person.setEmails(dto.emails());
        // person.setPhones(dto.phones());
        person.setTagKeys(dto.tagKeys());
        person.setSkillKeys(dto.skillKeys());
        person.setDepartmentKey(dto.departmentKey());
        person.setIronworkerNumber(dto.ironworkerNumber());
        person.setNotes(dto.notes());
        
        Person savedPerson = personsService.create(person, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(personToMap(savedPerson));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Person person = personsService.getById(id, orgId, includeArchived);
        
        return ResponseEntity.ok(personToMap(person));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePersonDto dto,
            @RequestParam(required = false) String orgId) {
        
                if (orgId == null) { return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); }
        Person personUpdates = new Person();
        personUpdates.setDisplayName(dto.displayName());
        personUpdates.setFirstName(dto.firstName());
        personUpdates.setLastName(dto.lastName());
        personUpdates.setPrimaryEmail(dto.primaryEmail());
        personUpdates.setPrimaryPhoneE164(dto.primaryPhone());
        personUpdates.setPersonType(dto.personType());
        personUpdates.setDateOfBirth(dto.dateOfBirth());
        personUpdates.setTagKeys(dto.tagKeys());
        personUpdates.setSkillKeys(dto.skillKeys());
        personUpdates.setDepartmentKey(dto.departmentKey());
        personUpdates.setIronworkerNumber(dto.ironworkerNumber());
        personUpdates.setNotes(dto.notes());
        
        Person updatedPerson = personsService.update(id, personUpdates, orgId);
        
        return ResponseEntity.ok(personToMap(updatedPerson));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Person archivedPerson = personsService.archive(id, orgId);
        
        return ResponseEntity.ok(personToMap(archivedPerson));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); 
        }
        Person unarchivedPerson = personsService.unarchive(id, orgId);
        
        return ResponseEntity.ok(personToMap(unarchivedPerson));
    }
    
    // Helper methods
    
    private Map<String, Object> personToMap(Person person) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", person.getId());
        map.put("firstName", person.getFirstName());
        map.put("lastName", person.getLastName());
        map.put("primaryEmail", person.getPrimaryEmail());
        map.put("primaryPhone", person.getPrimaryPhoneE164());
        map.put("title", person.getTitle());
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
    
        
}
