package com.mytegroup.api.service.persons;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.people.PersonRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for person management.
 * Handles CRUD operations, person relationships, and contact information.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonsService {
    
    private final PersonRepository personRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Creates a new person
     */
    @Transactional
    public Person create(Person person, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        Organization org = authHelper.validateOrg(orgId);
        person.setOrganization(org);
        
        // Validate display name
        if (person.getDisplayName() == null || person.getDisplayName().trim().isEmpty()) {
            throw new BadRequestException("Display name is required");
        }
        
        // Check for primary email uniqueness
        if (person.getPrimaryEmail() != null && !person.getPrimaryEmail().trim().isEmpty()) {
            String normalizedEmail = validationHelper.normalizeEmail(person.getPrimaryEmail());
            if (personRepository.findByOrganization_IdAndPrimaryEmail(org.getId(), normalizedEmail)
                .filter(p -> p.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Person primaryEmail already exists for this organization");
            }
            person.setPrimaryEmail(normalizedEmail);
        }
        
        // Check for primary phone uniqueness
        if (person.getPrimaryPhoneE164() != null && !person.getPrimaryPhoneE164().trim().isEmpty()) {
            String normalizedPhone = validationHelper.normalizePhoneE164(person.getPrimaryPhoneE164());
            if (normalizedPhone != null) {
                if (personRepository.findByOrganization_IdAndPrimaryPhoneE164(org.getId(), normalizedPhone)
                    .filter(p -> p.getArchivedAt() == null)
                    .isPresent()) {
                    throw new ConflictException("Person primaryPhone already exists for this organization");
                }
                person.setPrimaryPhoneE164(normalizedPhone);
            }
        }
        
        // Check for ironworker number uniqueness
        if (person.getIronworkerNumber() != null && !person.getIronworkerNumber().trim().isEmpty()) {
            if (personRepository.findByOrganization_IdAndIronworkerNumber(org.getId(), person.getIronworkerNumber())
                .filter(p -> p.getArchivedAt() == null)
                .isPresent()) {
                throw new ConflictException("Ironworker number already exists for this organization");
            }
        }
        
        Person savedPerson = personRepository.save(person);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("displayName", savedPerson.getDisplayName());
        metadata.put("personType", savedPerson.getPersonType() != null ? savedPerson.getPersonType().toString() : null);
        
        auditLogService.log(
            "person.created",
            orgId,
            null,
            "Person",
            savedPerson.getId().toString(),
            metadata
        );
        
        return savedPerson;
    }
    
    /**
     * Lists persons for an organization
     */
    @Transactional(readOnly = true)
    public List<Person> list(String orgId, boolean includeArchived) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Long orgIdLong = Long.parseLong(orgId);
        
        if (includeArchived) {
            return personRepository.findByOrganization_Id(orgIdLong);
        } else {
            return personRepository.findByOrganization_IdAndArchivedAtIsNull(orgIdLong, 
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        }
    }
    
    /**
     * Gets a person by ID
     */
    @Transactional(readOnly = true)
    public Person getById(Long id, String orgId, boolean includeArchived) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Person person = personRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        
        if (person.getOrganization() == null || 
            !person.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Person not found");
        }
        
        if (person.getArchivedAt() != null && !includeArchived) {
            throw new ResourceNotFoundException("Person archived");
        }
        
        return person;
    }
    
    /**
     * Updates a person
     */
    @Transactional
    public Person update(Long id, Person personUpdates, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Person person = personRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        
        if (person.getOrganization() == null || 
            !person.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Person not found");
        }
        
        if (person.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Person archived");
        }
        
        authHelper.ensureNotOnLegalHold(person, "update");
        
        // Update fields
        if (personUpdates.getDisplayName() != null) {
            person.setDisplayName(personUpdates.getDisplayName());
        }
        if (personUpdates.getFirstName() != null) {
            person.setFirstName(personUpdates.getFirstName());
        }
        if (personUpdates.getLastName() != null) {
            person.setLastName(personUpdates.getLastName());
        }
        if (personUpdates.getPersonType() != null) {
            person.setPersonType(personUpdates.getPersonType());
        }
        
        // Update primary email with uniqueness check
        if (personUpdates.getPrimaryEmail() != null) {
            String normalizedEmail = validationHelper.normalizeEmail(personUpdates.getPrimaryEmail());
            if (normalizedEmail != null && !normalizedEmail.equals(person.getPrimaryEmail())) {
                if (personRepository.findByOrganization_IdAndPrimaryEmail(person.getOrganization().getId(), normalizedEmail)
                    .filter(p -> !p.getId().equals(id) && p.getArchivedAt() == null)
                    .isPresent()) {
                    throw new ConflictException("Person primaryEmail already exists for this organization");
                }
                person.setPrimaryEmail(normalizedEmail);
            }
        }
        
        // Update primary phone with uniqueness check
        if (personUpdates.getPrimaryPhoneE164() != null) {
            String normalizedPhone = validationHelper.normalizePhoneE164(personUpdates.getPrimaryPhoneE164());
            if (normalizedPhone != null && !normalizedPhone.equals(person.getPrimaryPhoneE164())) {
                if (personRepository.findByOrganization_IdAndPrimaryPhoneE164(person.getOrganization().getId(), normalizedPhone)
                    .filter(p -> !p.getId().equals(id) && p.getArchivedAt() == null)
                    .isPresent()) {
                    throw new ConflictException("Person primaryPhone already exists for this organization");
                }
                person.setPrimaryPhoneE164(normalizedPhone);
            }
        }
        
        // Update other fields
        if (personUpdates.getEmails() != null) {
            person.setEmails(personUpdates.getEmails());
        }
        if (personUpdates.getPhones() != null) {
            person.setPhones(personUpdates.getPhones());
        }
        if (personUpdates.getTagKeys() != null) {
            person.setTagKeys(personUpdates.getTagKeys());
        }
        if (personUpdates.getSkillKeys() != null) {
            person.setSkillKeys(personUpdates.getSkillKeys());
        }
        if (personUpdates.getDepartmentKey() != null) {
            person.setDepartmentKey(personUpdates.getDepartmentKey());
        }
        if (personUpdates.getDateOfBirth() != null) {
            person.setDateOfBirth(personUpdates.getDateOfBirth());
        }
        
        Person savedPerson = personRepository.save(person);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", Map.of("updated", true));
        
        auditLogService.log(
            "person.updated",
            orgId,
            null,
            "Person",
            savedPerson.getId().toString(),
            metadata
        );
        
        return savedPerson;
    }
    
    /**
     * Archives a person
     */
    @Transactional
    public Person archive(Long id, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Person person = personRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        
        if (person.getOrganization() == null || 
            !person.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Person not found");
        }
        
        authHelper.ensureNotOnLegalHold(person, "archive");
        
        if (person.getArchivedAt() != null) {
            return person;
        }
        
        person.setArchivedAt(LocalDateTime.now());
        Person savedPerson = personRepository.save(person);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedPerson.getArchivedAt());
        
        auditLogService.log(
            "person.archived",
            orgId,
            null,
            "Person",
            savedPerson.getId().toString(),
            metadata
        );
        
        return savedPerson;
    }
    
    /**
     * Unarchives a person
     */
    @Transactional
    public Person unarchive(Long id, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Person person = personRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        
        if (person.getOrganization() == null || 
            !person.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Person not found");
        }
        
        authHelper.ensureNotOnLegalHold(person, "unarchive");
        
        if (person.getArchivedAt() == null) {
            return person;
        }
        
        person.setArchivedAt(null);
        Person savedPerson = personRepository.save(person);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedPerson.getArchivedAt());
        
        auditLogService.log(
            "person.unarchived",
            orgId,
            null,
            "Person",
            savedPerson.getId().toString(),
            metadata
        );
        
        return savedPerson;
    }
    
    /**
     * Finds a person by primary email.
     * Returns null if person not found (for existence checks).
     * @throws BadRequestException if email is invalid
     */
    @Transactional(readOnly = true)
    public Person findByPrimaryEmail(String orgId, String email) {
        String normalizedEmail = validationHelper.normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        Long orgIdLong = Long.parseLong(orgId);
        
        return personRepository.findByOrganization_IdAndPrimaryEmail(orgIdLong, normalizedEmail)
            .filter(p -> p.getArchivedAt() == null)
            .orElse(null);
    }
    
    /**
     * Links a user to a person
     */
    @Transactional
    public Person linkUser(String orgId, Long personId, Long userId) {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        
        // User relationship is set via the user entity's person relationship
        // This method is kept for API compatibility but the relationship
        // should be managed through the User entity
        
        return personRepository.save(person);
    }
}

