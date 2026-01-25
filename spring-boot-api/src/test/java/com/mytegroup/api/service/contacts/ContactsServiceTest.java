package com.mytegroup.api.service.contacts;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.Contact;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.repository.people.ContactRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactsServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private ServiceValidationHelper validationHelper;

    @InjectMocks
    private ContactsService contactsService;

    private Organization testOrganization;
    private Contact testContact;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testContact = new Contact();
        testContact.setId(1L);
        testContact.setName("Test Contact");
        testContact.setEmail("contact@example.com");
        testContact.setOrganization(testOrganization);
    }

    @Test
    void testCreate_WithValidContact_CreatesContact() {
        Contact newContact = new Contact();
        newContact.setName("New Contact");
        newContact.setEmail("newcontact@example.com");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeEmail("newcontact@example.com")).thenReturn("newcontact@example.com");
        when(contactRepository.findByOrganization_IdAndEmail(1L, "newcontact@example.com")).thenReturn(List.of());
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            contact.setId(1L);
            return contact;
        });
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), anyString(), any());

        Contact result = contactsService.create(newContact, "1");

        assertNotNull(result);
        assertEquals(testOrganization, result.getOrganization());
        verify(contactRepository, times(1)).save(any(Contact.class));
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        Contact newContact = new Contact();

        assertThrows(BadRequestException.class, () -> {
            contactsService.create(newContact, null);
        });
    }

    @Test
    void testCreate_WithDuplicateEmail_ThrowsConflictException() {
        Contact newContact = new Contact();
        newContact.setEmail("existing@example.com");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeEmail("existing@example.com")).thenReturn("existing@example.com");
        when(contactRepository.findByOrganization_IdAndEmail(1L, "existing@example.com")).thenReturn(List.of(testContact));

        assertThrows(ConflictException.class, () -> {
            contactsService.create(newContact, "1");
        });
    }

    @Test
    void testCreate_WithDuplicateIronworkerNumber_ThrowsConflictException() {
        Contact newContact = new Contact();
        newContact.setIronworkerNumber("IW123");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(contactRepository.findByOrganization_IdAndIronworkerNumber(1L, "IW123")).thenReturn(List.of(testContact));

        assertThrows(ConflictException.class, () -> {
            contactsService.create(newContact, "1");
        });
    }

    @Test
    void testList_WithValidParams_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(contactRepository.findByOrganization_IdAndArchivedAtIsNull(1L, pageable)).thenReturn(Page.empty());

        Page<Contact> result = contactsService.list("1", null, null, null, false, 0, 10);

        assertNotNull(result);
        verify(contactRepository, times(1)).findByOrganization_IdAndArchivedAtIsNull(1L, pageable);
    }

    @Test
    void testList_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            contactsService.list(null, null, null, null, false, 0, 10);
        });
    }

    @Test
    void testGetById_WithValidId_ReturnsContact() {
        Long contactId = 1L;
        when(contactRepository.findById(contactId)).thenReturn(Optional.of(testContact));

        Contact result = contactsService.getById(contactId, "1", false);

        assertNotNull(result);
        assertEquals(contactId, result.getId());
    }

    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long contactId = 999L;
        when(contactRepository.findById(contactId)).thenReturn(Optional.empty());

        assertThrows(com.mytegroup.api.exception.ResourceNotFoundException.class, () -> {
            contactsService.getById(contactId, "1", false);
        });
    }
}

