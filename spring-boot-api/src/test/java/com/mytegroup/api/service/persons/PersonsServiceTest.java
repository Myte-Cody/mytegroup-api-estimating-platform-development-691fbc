package com.mytegroup.api.service.persons;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.people.PersonType;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.people.PersonRepository;
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
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonsServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private ServiceValidationHelper validationHelper;

    @InjectMocks
    private PersonsService personsService;

    private Organization testOrganization;
    private Person testPerson;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testPerson = new Person();
        testPerson.setId(1L);
        testPerson.setDisplayName("Test Person");
        testPerson.setPrimaryEmail("person@example.com");
        testPerson.setPersonType(PersonType.INTERNAL_STAFF);
        testPerson.setOrganization(testOrganization);
    }

    @Test
    void testCreate_WithValidPerson_CreatesPerson() {
        Person newPerson = new Person();
        newPerson.setDisplayName("New Person");
        newPerson.setPrimaryEmail("newperson@example.com");
        newPerson.setPersonType(PersonType.INTERNAL_STAFF);

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeEmail("newperson@example.com")).thenReturn("newperson@example.com");
        when(personRepository.findByOrganization_IdAndPrimaryEmail(1L, "newperson@example.com"))
            .thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            person.setId(1L);
            return person;
        });
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), anyString(), anyString(), any());

        Person result = personsService.create(newPerson, "1");

        assertNotNull(result);
        assertEquals(testOrganization, result.getOrganization());
        verify(personRepository, times(1)).save(any(Person.class));
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        Person newPerson = new Person();

        assertThrows(BadRequestException.class, () -> {
            personsService.create(newPerson, null);
        });
    }

    @Test
    void testCreate_WithEmptyDisplayName_ThrowsBadRequestException() {
        Person newPerson = new Person();
        newPerson.setDisplayName("   ");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);

        assertThrows(BadRequestException.class, () -> {
            personsService.create(newPerson, "1");
        });
    }

    @Test
    void testCreate_WithDuplicateEmail_ThrowsConflictException() {
        Person newPerson = new Person();
        newPerson.setDisplayName("New Person");
        newPerson.setPrimaryEmail("person@example.com");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(validationHelper.normalizeEmail("person@example.com")).thenReturn("person@example.com");
        when(personRepository.findByOrganization_IdAndPrimaryEmail(1L, "person@example.com"))
            .thenReturn(Optional.of(testPerson));

        assertThrows(ConflictException.class, () -> {
            personsService.create(newPerson, "1");
        });
    }

    @Test
    void testList_WithValidParams_ReturnsList() {
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(personRepository.findByOrganization_IdAndArchivedAtIsNull(eq(1L), any(PageRequest.class)))
            .thenReturn(org.springframework.data.domain.Page.empty());

        List<Person> result = personsService.list("1", false);

        assertNotNull(result);
        verify(personRepository, times(1)).findByOrganization_IdAndArchivedAtIsNull(eq(1L), any(PageRequest.class));
    }

    @Test
    void testList_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            personsService.list(null, false);
        });
    }

    @Test
    void testGetById_WithValidId_ReturnsPerson() {
        Long personId = 1L;
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(personRepository.findById(personId)).thenReturn(Optional.of(testPerson));

        Person result = personsService.getById(personId, "1", false);

        assertNotNull(result);
        assertEquals(personId, result.getId());
    }

    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long personId = 999L;
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            personsService.getById(personId, "1", false);
        });
    }
}


