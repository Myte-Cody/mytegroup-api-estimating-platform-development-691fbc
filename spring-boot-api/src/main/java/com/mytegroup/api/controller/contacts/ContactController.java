package com.mytegroup.api.controller.contacts;

import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.contacts.ContactsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ContactController {

    private final ContactsService contactsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String personType,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Page<Person> contacts = contactsService.list(actor, resolvedOrgId, search, personType, companyId, includeArchived, page, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", contacts.getContent().stream().map(this::contactToMap).toList());
        response.put("total", contacts.getTotalElements());
        response.put("page", page);
        response.put("limit", limit);
        
        return response;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public Map<String, Object> getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        ActorContext actor = getActorContext();
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        
        Person contact = contactsService.getById(id, actor, resolvedOrgId, includeArchived);
        
        return contactToMap(contact);
    }
    
    // Helper methods
    
    private Map<String, Object> contactToMap(Person contact) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", contact.getId());
        map.put("firstName", contact.getFirstName());
        map.put("lastName", contact.getLastName());
        map.put("primaryEmail", contact.getPrimaryEmail());
        map.put("primaryPhone", contact.getPrimaryPhone());
        map.put("jobTitle", contact.getJobTitle());
        map.put("personType", contact.getPersonType() != null ? contact.getPersonType().getValue() : null);
        map.put("notes", contact.getNotes());
        map.put("externalId", contact.getExternalId());
        map.put("companyId", contact.getCompany() != null ? contact.getCompany().getId() : null);
        map.put("companyName", contact.getCompany() != null ? contact.getCompany().getName() : null);
        map.put("piiStripped", contact.getPiiStripped());
        map.put("legalHold", contact.getLegalHold());
        map.put("archivedAt", contact.getArchivedAt());
        map.put("orgId", contact.getOrganization() != null ? contact.getOrganization().getId() : null);
        map.put("createdAt", contact.getCreatedAt());
        map.put("updatedAt", contact.getUpdatedAt());
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
