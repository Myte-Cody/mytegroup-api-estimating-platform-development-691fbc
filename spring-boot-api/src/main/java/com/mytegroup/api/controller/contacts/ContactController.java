package com.mytegroup.api.controller.contacts;

import com.mytegroup.api.entity.people.Contact;
import com.mytegroup.api.service.contacts.ContactsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
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
        
        if (orgId == null) {
            return Map.of("error", "orgId is required", "data", java.util.List.of(), "total", 0, "page", page, "limit", limit);
        }
        
        org.springframework.data.domain.Page<Contact> contacts = contactsService.list(orgId, search, personType, companyId, includeArchived, page, limit);
        
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
        
        Contact contact = contactsService.getById(id, orgId, includeArchived);
        
        return contactToMap(contact);
    }
    
    // Helper methods
    
    private Map<String, Object> contactToMap(Contact contact) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", contact.getId());
        map.put("name", contact.getName());
        map.put("firstName", contact.getFirstName());
        map.put("lastName", contact.getLastName());
        map.put("email", contact.getEmail());
        map.put("phone", contact.getPhone());
        map.put("personType", contact.getPersonType() != null ? contact.getPersonType().getValue() : null);
        map.put("notes", contact.getNotes());
        map.put("ironworkerNumber", contact.getIronworkerNumber());
        map.put("company", contact.getCompany());
        map.put("piiStripped", contact.getPiiStripped());
        map.put("legalHold", contact.getLegalHold());
        map.put("archivedAt", contact.getArchivedAt());
        map.put("orgId", contact.getOrganization() != null ? contact.getOrganization().getId() : null);
        map.put("createdAt", contact.getCreatedAt());
        map.put("updatedAt", contact.getUpdatedAt());
        return map;
    }
    
}
