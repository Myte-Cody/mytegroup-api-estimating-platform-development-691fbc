package com.mytegroup.api.controller.persons;

import com.mytegroup.api.dto.persons.*;
import com.mytegroup.api.dto.response.PersonResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.mapper.persons.PersonMapper;
import com.mytegroup.api.mapper.response.PersonResponseMapper;
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
    private final PersonMapper personMapper;
    private final PersonResponseMapper personResponseMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PaginatedResponseDto<PersonResponseDto>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String personType,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) { 
            return ResponseEntity.badRequest().body(PaginatedResponseDto.<PersonResponseDto>builder()
                    .data(List.of())
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .build());
        }
        List<Person> persons = personsService.list(orgId, includeArchived != null && includeArchived);
        
        return ResponseEntity.ok(PaginatedResponseDto.<PersonResponseDto>builder()
                .data(persons.stream().map(personResponseMapper::toDto).toList())
                .total(persons.size())
                .page(page)
                .limit(limit)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PersonResponseDto> create(
            @RequestBody @Valid CreatePersonDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Person person = personMapper.toEntity(dto, null, null, null, null, null);
        
        Person savedPerson = personsService.create(person, orgId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(personResponseMapper.toDto(savedPerson));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PersonResponseDto> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Person person = personsService.getById(id, orgId, includeArchived);
        
        return ResponseEntity.ok(personResponseMapper.toDto(person));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PersonResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePersonDto dto,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Person personUpdates = new Person();
        personMapper.updateEntity(personUpdates, dto, null, null, null, null);
        
        Person updatedPerson = personsService.update(id, personUpdates, orgId);
        
        return ResponseEntity.ok(personResponseMapper.toDto(updatedPerson));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PersonResponseDto> archive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Person archivedPerson = personsService.archive(id, orgId);
        
        return ResponseEntity.ok(personResponseMapper.toDto(archivedPerson));
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PersonResponseDto> unarchive(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId) {
        
        if (orgId == null) { 
            throw new IllegalArgumentException("orgId is required");
        }
        Person unarchivedPerson = personsService.unarchive(id, orgId);
        
        return ResponseEntity.ok(personResponseMapper.toDto(unarchivedPerson));
    }
    
}
