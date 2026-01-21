package com.mytegroup.api.service.contacts;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.Contact;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.people.ContactRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for contact management.
 * Handles CRUD operations for contacts (external/internal staff).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactsService {
    
    private final ContactRepository contactRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Creates a new contact
     */
    @Transactional
    public Contact create(Contact contact, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.PM, Role.ORG_OWNER);
        authHelper.ensureOrgScope(orgId, actor);
        Organization org = authHelper.validateOrg(orgId);
        contact.setOrganization(org);
        
        Long orgIdLong = Long.parseLong(orgId);
        
        // Check email uniqueness
        if (contact.getEmail() != null && !contact.getEmail().trim().isEmpty()) {
            String normalizedEmail = validationHelper.normalizeEmail(contact.getEmail());
            if (!contactRepository.findByOrgIdAndEmail(orgIdLong, normalizedEmail).isEmpty()) {
                throw new ConflictException("Contact email already exists for this organization");
            }
            contact.setEmail(normalizedEmail);
        }
        
        // Check ironworker number uniqueness
        if (contact.getIronworkerNumber() != null && !contact.getIronworkerNumber().trim().isEmpty()) {
            if (!contactRepository.findByOrgIdAndIronworkerNumber(orgIdLong, contact.getIronworkerNumber()).isEmpty()) {
                throw new ConflictException("Ironworker number already exists for this organization");
            }
        }
        
        // Set defaults
        if (contact.getPromotedToForeman() == null) {
            contact.setPromotedToForeman(false);
        }
        
        Contact savedContact = contactRepository.save(contact);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", savedContact.getName());
        metadata.put("email", savedContact.getEmail());
        
        auditLogService.log(
            "contact.created",
            orgId,
            actor != null ? actor.getUserId() : null,
            "Contact",
            savedContact.getId().toString(),
            metadata
        );
        
        return savedContact;
    }
    
    /**
     * Lists contacts for an organization
     */
    @Transactional(readOnly = true)
    public List<Contact> list(ActorContext actor, String orgId, boolean includeArchived, String personType) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER, Role.PM);
        authHelper.ensureOrgScope(orgId, actor);
        
        if (includeArchived && !authHelper.canViewArchived(actor)) {
            throw new ForbiddenException("Not allowed to include archived contacts");
        }
        
        Long orgIdLong = Long.parseLong(orgId);
        
        // TODO: Filter by personType if provided
        if (includeArchived) {
            return contactRepository.findByOrgId(orgIdLong);
        } else {
            return contactRepository.findByOrgIdAndArchivedAtIsNull(orgIdLong);
        }
    }
    
    /**
     * Gets a contact by ID
     */
    @Transactional(readOnly = true)
    public Contact getById(Long id, ActorContext actor, String orgId, boolean includeArchived) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER, Role.PM);
        
        if (orgId == null && actor.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Organization context is required");
        }
        
        String resolvedOrg = orgId != null ? orgId : actor.getOrgId();
        if (resolvedOrg == null) {
            throw new ForbiddenException("Organization context is required");
        }
        authHelper.ensureOrgScope(resolvedOrg, actor);
        
        Contact contact = contactRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        
        if (contact.getOrganization() == null || 
            !contact.getOrganization().getId().toString().equals(resolvedOrg)) {
            throw new ResourceNotFoundException("Contact not found");
        }
        
        if (contact.getArchivedAt() != null && !includeArchived) {
            throw new ResourceNotFoundException("Contact archived");
        }
        
        if (contact.getArchivedAt() != null && includeArchived && !authHelper.canViewArchived(actor)) {
            throw new ForbiddenException("Not allowed to view archived contacts");
        }
        
        return contact;
    }
    
    /**
     * Updates a contact
     */
    @Transactional
    public Contact update(Long id, Contact contactUpdates, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.PM, Role.ORG_OWNER);
        authHelper.ensureOrgScope(orgId, actor);
        authHelper.validateOrg(orgId);
        
        Contact contact = contactRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        
        if (contact.getOrganization() == null || 
            !contact.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Contact not found");
        }
        
        if (contact.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Contact archived");
        }
        
        authHelper.ensureNotOnLegalHold(contact, "update");
        
        Long orgIdLong = Long.parseLong(orgId);
        
        // Update email with uniqueness check
        if (contactUpdates.getEmail() != null && !contactUpdates.getEmail().equals(contact.getEmail())) {
            String normalizedEmail = validationHelper.normalizeEmail(contactUpdates.getEmail());
            List<Contact> existing = contactRepository.findByOrgIdAndEmail(orgIdLong, normalizedEmail);
            if (!existing.isEmpty() && existing.stream().anyMatch(c -> !c.getId().equals(id))) {
                throw new ConflictException("Contact email already exists for this organization");
            }
            contact.setEmail(normalizedEmail);
        }
        
        // Update ironworker number with uniqueness check
        if (contactUpdates.getIronworkerNumber() != null && 
            !contactUpdates.getIronworkerNumber().equals(contact.getIronworkerNumber())) {
            List<Contact> existing = contactRepository.findByOrgIdAndIronworkerNumber(orgIdLong, contactUpdates.getIronworkerNumber());
            if (!existing.isEmpty() && existing.stream().anyMatch(c -> !c.getId().equals(id))) {
                throw new ConflictException("Ironworker number already exists for this organization");
            }
            contact.setIronworkerNumber(contactUpdates.getIronworkerNumber());
        }
        
        // Update other fields
        if (contactUpdates.getName() != null) {
            contact.setName(contactUpdates.getName());
        }
        if (contactUpdates.getPersonType() != null) {
            contact.setPersonType(contactUpdates.getPersonType());
        }
        if (contactUpdates.getContactKind() != null) {
            contact.setContactKind(contactUpdates.getContactKind());
        }
        if (contactUpdates.getFirstName() != null) {
            contact.setFirstName(contactUpdates.getFirstName());
        }
        if (contactUpdates.getLastName() != null) {
            contact.setLastName(contactUpdates.getLastName());
        }
        if (contactUpdates.getDisplayName() != null) {
            contact.setDisplayName(contactUpdates.getDisplayName());
        }
        if (contactUpdates.getDateOfBirth() != null) {
            contact.setDateOfBirth(contactUpdates.getDateOfBirth());
        }
        if (contactUpdates.getUnionLocal() != null) {
            contact.setUnionLocal(contactUpdates.getUnionLocal());
        }
        if (contactUpdates.getPromotedToForeman() != null) {
            contact.setPromotedToForeman(contactUpdates.getPromotedToForeman());
        }
        if (contactUpdates.getPhone() != null) {
            contact.setPhone(contactUpdates.getPhone());
        }
        if (contactUpdates.getCompany() != null) {
            contact.setCompany(contactUpdates.getCompany());
        }
        if (contactUpdates.getRoles() != null) {
            contact.setRoles(contactUpdates.getRoles());
        }
        if (contactUpdates.getTags() != null) {
            contact.setTags(contactUpdates.getTags());
        }
        if (contactUpdates.getNotes() != null) {
            contact.setNotes(contactUpdates.getNotes());
        }
        if (contactUpdates.getRating() != null) {
            contact.setRating(contactUpdates.getRating());
        }
        if (contactUpdates.getCertifications() != null) {
            contact.setCertifications(contactUpdates.getCertifications());
        }
        if (contactUpdates.getSkills() != null) {
            contact.setSkills(contactUpdates.getSkills());
        }
        
        Contact savedContact = contactRepository.save(contact);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", Map.of("updated", true));
        
        auditLogService.log(
            "contact.updated",
            orgId,
            actor != null ? actor.getUserId() : null,
            "Contact",
            savedContact.getId().toString(),
            metadata
        );
        
        return savedContact;
    }
    
    /**
     * Archives a contact
     */
    @Transactional
    public Contact archive(Long id, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER);
        authHelper.ensureOrgScope(orgId, actor);
        authHelper.validateOrg(orgId);
        
        Contact contact = contactRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        
        if (contact.getOrganization() == null || 
            !contact.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Contact not found");
        }
        
        authHelper.ensureNotOnLegalHold(contact, "archive");
        
        if (contact.getArchivedAt() != null) {
            return contact;
        }
        
        contact.setArchivedAt(LocalDateTime.now());
        Contact savedContact = contactRepository.save(contact);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedContact.getArchivedAt());
        
        auditLogService.log(
            "contact.archived",
            orgId,
            actor != null ? actor.getUserId() : null,
            "Contact",
            savedContact.getId().toString(),
            metadata
        );
        
        return savedContact;
    }
    
    /**
     * Unarchives a contact
     */
    @Transactional
    public Contact unarchive(Long id, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.ADMIN, Role.MANAGER, Role.ORG_OWNER);
        authHelper.ensureOrgScope(orgId, actor);
        authHelper.validateOrg(orgId);
        
        Contact contact = contactRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        
        if (contact.getOrganization() == null || 
            !contact.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Contact not found");
        }
        
        authHelper.ensureNotOnLegalHold(contact, "unarchive");
        
        if (contact.getArchivedAt() == null) {
            return contact;
        }
        
        contact.setArchivedAt(null);
        Contact savedContact = contactRepository.save(contact);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedAt", savedContact.getArchivedAt());
        
        auditLogService.log(
            "contact.unarchived",
            orgId,
            actor != null ? actor.getUserId() : null,
            "Contact",
            savedContact.getId().toString(),
            metadata
        );
        
        return savedContact;
    }
    
    /**
     * Links an invited user to a contact
     */
    @Transactional
    public Contact linkInvitedUser(String orgId, Long contactId, Long userId) {
        authHelper.validateOrg(orgId);
        
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        
        if (contact.getOrganization() == null || 
            !contact.getOrganization().getId().toString().equals(orgId)) {
            throw new ResourceNotFoundException("Contact not found");
        }
        
        if (Boolean.TRUE.equals(contact.getLegalHold())) {
            throw new ForbiddenException("Contact on legal hold");
        }
        
        // TODO: Set invitedUserId and inviteStatus - requires User entity relationship
        // contact.setInvitedUserId(userId);
        // contact.setInviteStatus(InviteStatus.ACCEPTED);
        // contact.setInvitedAt(LocalDateTime.now());
        
        Contact savedContact = contactRepository.save(contact);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invitedUserId", userId.toString());
        
        auditLogService.log(
            "contact.invite_linked",
            orgId,
            null,
            "Contact",
            contactId.toString(),
            metadata
        );
        
        return savedContact;
    }
}

