package com.mytegroup.api.controller.contacts;

import com.mytegroup.api.dto.response.ContactResponseDto;
import com.mytegroup.api.dto.response.PaginatedResponseDto;
import com.mytegroup.api.entity.people.Contact;
import com.mytegroup.api.mapper.response.ContactResponseMapper;
import com.mytegroup.api.service.contacts.ContactsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ContactController {

    private final ContactsService contactsService;
    private final ContactResponseMapper contactResponseMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public PaginatedResponseDto<ContactResponseDto> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String personType,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        
        if (orgId == null) {
            return PaginatedResponseDto.<ContactResponseDto>builder()
                    .data(java.util.List.of())
                    .total(0)
                    .page(page)
                    .limit(limit)
                    .build();
        }
        
        org.springframework.data.domain.Page<Contact> contacts = contactsService.list(orgId, search, personType, companyId, includeArchived, page, limit);
        
        return PaginatedResponseDto.<ContactResponseDto>builder()
                .data(contacts.getContent().stream().map(contactResponseMapper::toDto).toList())
                .total(contacts.getTotalElements())
                .page(page)
                .limit(limit)
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ContactResponseDto getById(
            @PathVariable Long id,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        
        Contact contact = contactsService.getById(id, orgId, includeArchived);
        
        return contactResponseMapper.toDto(contact);
    }
    
}
