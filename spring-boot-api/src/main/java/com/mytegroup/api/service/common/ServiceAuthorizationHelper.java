package com.mytegroup.api.service.common;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class for common authorization checks used across services.
 * Provides methods for role validation, organization scoping, and legal hold checks.
 */
@Component
@RequiredArgsConstructor
public class ServiceAuthorizationHelper {
    
    private final OrganizationRepository organizationRepository;
    
    /**
     * Ensures the actor has one of the allowed roles (considering role expansion)
     * @throws ForbiddenException if actor doesn't have required role
     */
    public void ensureRole(ActorContext actor, Role... allowedRoles) {
        if (actor == null || actor.getRole() == null) {
            throw new ForbiddenException("Insufficient role");
        }
        
        if (actor.getRole() == Role.SUPER_ADMIN) {
            return; // SuperAdmin has all permissions
        }
        
        if (!RoleExpansionHelper.hasAnyRole(actor.getRole(), Arrays.asList(allowedRoles))) {
            throw new ForbiddenException("Insufficient role");
        }
    }
    
    /**
     * Ensures the actor has one of the allowed roles (considering role expansion)
     * @throws ForbiddenException if actor doesn't have required role
     */
    public void ensureRole(ActorContext actor, List<Role> allowedRoles) {
        if (actor == null || actor.getRole() == null) {
            throw new ForbiddenException("Insufficient role");
        }
        
        if (actor.getRole() == Role.SUPER_ADMIN) {
            return; // SuperAdmin has all permissions
        }
        
        if (!RoleExpansionHelper.hasAnyRole(actor.getRole(), allowedRoles)) {
            throw new ForbiddenException("Insufficient role");
        }
    }
    
    /**
     * Resolves the organization ID, considering actor's role and requested orgId
     * SuperAdmin can access any org, others are restricted to their own org
     */
    public String resolveOrgId(String requestedOrgId, ActorContext actor) {
        if (actor.getRole() == Role.SUPER_ADMIN && requestedOrgId != null) {
            return requestedOrgId;
        }
        if (actor.getOrgId() != null) {
            return actor.getOrgId();
        }
        throw new ForbiddenException("Missing organization context");
    }
    
    /**
     * Ensures the actor can access the specified organization
     * SuperAdmin and PlatformAdmin can access any org, others only their own
     * @throws ForbiddenException if actor cannot access the organization
     */
    public void ensureOrgScope(String orgId, ActorContext actor) {
        if (actor == null) {
            throw new ForbiddenException("Missing actor context");
        }
        
        if (actor.getRole() == Role.SUPER_ADMIN || actor.getRole() == Role.PLATFORM_ADMIN) {
            return; // Platform admins can access any org
        }
        
        if (actor.getOrgId() == null || !actor.getOrgId().equals(orgId)) {
            throw new ForbiddenException("Cannot access resources outside your organization");
        }
    }
    
    /**
     * Checks if the actor can view archived entities
     */
    public boolean canViewArchived(ActorContext actor) {
        if (actor == null || actor.getRole() == null) {
            return false;
        }
        
        List<Role> effectiveRoles = RoleExpansionHelper.expandRoles(actor.getRole());
        List<Role> allowedRoles = List.of(
            Role.SUPER_ADMIN,
            Role.PLATFORM_ADMIN,
            Role.ORG_OWNER,
            Role.ORG_ADMIN,
            Role.ADMIN
        );
        return allowedRoles.stream().anyMatch(effectiveRoles::contains);
    }
    
    /**
     * Ensures the entity is not on legal hold
     * @throws ForbiddenException if entity is on legal hold
     */
    public void ensureNotOnLegalHold(Object entity, String action) {
        if (entity == null) {
            return;
        }
        
        try {
            // Try to get legalHold field using reflection
            Method getLegalHold = entity.getClass().getMethod("getLegalHold");
            Boolean legalHold = (Boolean) getLegalHold.invoke(entity);
            
            if (Boolean.TRUE.equals(legalHold)) {
                throw new ForbiddenException("Cannot " + action + " while legal hold is active");
            }
        } catch (NoSuchMethodException | ReflectiveOperationException e) {
            // Entity doesn't have legalHold field, skip check
        }
    }
    
    /**
     * Validates that an organization exists and is not archived
     * @throws ResourceNotFoundException if organization not found or archived
     * @throws ForbiddenException if organization is on legal hold
     */
    public Organization validateOrg(String orgId) {
        if (orgId == null) {
            throw new ResourceNotFoundException("Organization ID is required");
        }
        
        Long orgIdLong;
        try {
            orgIdLong = Long.parseLong(orgId);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Invalid organization ID format");
        }
        
        Organization org = organizationRepository.findById(orgIdLong)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found or archived"));
        
        if (org.getArchivedAt() != null) {
            throw new ResourceNotFoundException("Organization not found or archived");
        }
        
        if (Boolean.TRUE.equals(org.getLegalHold())) {
            throw new ForbiddenException("Organization is under legal hold");
        }
        
        return org;
    }
    
    /**
     * Validates that an organization exists (allows archived)
     * @throws ResourceNotFoundException if organization not found
     */
    public Organization validateOrgExists(String orgId) {
        if (orgId == null) {
            throw new ResourceNotFoundException("Organization ID is required");
        }
        
        Long orgIdLong;
        try {
            orgIdLong = Long.parseLong(orgId);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Invalid organization ID format");
        }
        
        return organizationRepository.findById(orgIdLong)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
}

