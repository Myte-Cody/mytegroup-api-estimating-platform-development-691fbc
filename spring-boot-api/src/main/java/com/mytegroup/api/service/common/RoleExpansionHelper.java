package com.mytegroup.api.service.common;

import com.mytegroup.api.common.enums.Role;

import java.util.*;

/**
 * Helper class for role expansion and hierarchy management.
 * Implements the role hierarchy logic from NestJS roles.ts
 */
public class RoleExpansionHelper {
    
    private static final List<Role> ROLE_PRIORITY = List.of(
        Role.SUPER_ADMIN,
        Role.PLATFORM_ADMIN,
        Role.ORG_OWNER,
        Role.ORG_ADMIN,
        Role.ADMIN,
        Role.MANAGER,
        Role.COMPLIANCE_OFFICER,
        Role.SECURITY_OFFICER,
        Role.PM,
        Role.ESTIMATOR,
        Role.ENGINEER,
        Role.DETAILER,
        Role.TRANSPORTER,
        Role.FOREMAN,
        Role.SUPERINTENDENT,
        Role.QAQC,
        Role.HS,
        Role.PURCHASING,
        Role.COMPLIANCE,
        Role.SECURITY,
        Role.FINANCE,
        Role.VIEWER,
        Role.USER
    );
    
    private static final List<Role> BASE_ROLES = List.of(
        Role.MANAGER,
        Role.COMPLIANCE_OFFICER,
        Role.SECURITY_OFFICER,
        Role.PM,
        Role.ESTIMATOR,
        Role.ENGINEER,
        Role.DETAILER,
        Role.TRANSPORTER,
        Role.FOREMAN,
        Role.SUPERINTENDENT,
        Role.QAQC,
        Role.HS,
        Role.PURCHASING,
        Role.COMPLIANCE,
        Role.SECURITY,
        Role.FINANCE,
        Role.VIEWER,
        Role.USER
    );
    
    private static final Map<Role, List<Role>> ROLE_HIERARCHY = new HashMap<>();
    
    static {
        ROLE_HIERARCHY.put(Role.SUPER_ADMIN, new ArrayList<>(ROLE_PRIORITY));
        ROLE_HIERARCHY.put(Role.PLATFORM_ADMIN, ROLE_PRIORITY.stream()
            .filter(r -> r != Role.SUPER_ADMIN)
            .toList());
        ROLE_HIERARCHY.put(Role.ORG_OWNER, new ArrayList<>(List.of(
            Role.ORG_OWNER, Role.ORG_ADMIN, Role.ADMIN
        )).stream().distinct().toList());
        ROLE_HIERARCHY.get(Role.ORG_OWNER).addAll(BASE_ROLES);
        ROLE_HIERARCHY.put(Role.ORG_ADMIN, new ArrayList<>(List.of(
            Role.ORG_ADMIN, Role.ADMIN
        )).stream().distinct().toList());
        ROLE_HIERARCHY.get(Role.ORG_ADMIN).addAll(BASE_ROLES);
        ROLE_HIERARCHY.put(Role.ADMIN, new ArrayList<>(List.of(Role.ADMIN)));
        ROLE_HIERARCHY.get(Role.ADMIN).addAll(BASE_ROLES);
        ROLE_HIERARCHY.put(Role.MANAGER, List.of(Role.MANAGER, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.VIEWER, List.of(Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.COMPLIANCE_OFFICER, List.of(Role.COMPLIANCE_OFFICER, Role.COMPLIANCE, Role.USER));
        ROLE_HIERARCHY.put(Role.SECURITY_OFFICER, List.of(Role.SECURITY_OFFICER, Role.SECURITY, Role.USER));
        ROLE_HIERARCHY.put(Role.PM, List.of(Role.PM, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.ESTIMATOR, List.of(Role.ESTIMATOR, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.ENGINEER, List.of(Role.ENGINEER, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.DETAILER, List.of(Role.DETAILER, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.TRANSPORTER, List.of(Role.TRANSPORTER, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.FOREMAN, List.of(Role.FOREMAN, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.SUPERINTENDENT, List.of(Role.SUPERINTENDENT, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.QAQC, List.of(Role.QAQC, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.HS, List.of(Role.HS, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.PURCHASING, List.of(Role.PURCHASING, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.COMPLIANCE, List.of(Role.COMPLIANCE, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.SECURITY, List.of(Role.SECURITY, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.FINANCE, List.of(Role.FINANCE, Role.VIEWER, Role.USER));
        ROLE_HIERARCHY.put(Role.USER, List.of(Role.USER));
    }
    
    /**
     * Expands a list of roles to include all implied roles based on hierarchy
     */
    public static List<Role> expandRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(Role.USER);
        }
        
        Set<Role> expanded = new HashSet<>();
        for (Role role : roles) {
            if (role != null) {
                expanded.add(role);
                List<Role> implied = ROLE_HIERARCHY.getOrDefault(role, List.of(role));
                expanded.addAll(implied);
            }
        }
        return new ArrayList<>(expanded);
    }
    
    /**
     * Expands a single role to include all implied roles
     */
    public static List<Role> expandRoles(Role role) {
        if (role == null) {
            return List.of(Role.USER);
        }
        return expandRoles(List.of(role));
    }
    
    /**
     * Checks if the actor has any of the allowed roles (considering role expansion)
     */
    public static boolean hasAnyRole(Role actorRole, Role... allowedRoles) {
        if (actorRole == null) {
            return false;
        }
        List<Role> actorEffective = expandRoles(actorRole);
        List<Role> allowed = Arrays.asList(allowedRoles);
        return allowed.stream().anyMatch(actorEffective::contains);
    }
    
    /**
     * Checks if the actor has any of the allowed roles (considering role expansion)
     */
    public static boolean hasAnyRole(Role actorRole, List<Role> allowedRoles) {
        if (actorRole == null || allowedRoles == null || allowedRoles.isEmpty()) {
            return false;
        }
        List<Role> actorEffective = expandRoles(actorRole);
        return allowedRoles.stream().anyMatch(actorEffective::contains);
    }
    
    /**
     * Gets the role priority list (highest priority first)
     */
    public static List<Role> getRolePriority() {
        return new ArrayList<>(ROLE_PRIORITY);
    }
    
    /**
     * Gets the role hierarchy map
     */
    public static Map<Role, List<Role>> getRoleHierarchy() {
        return new HashMap<>(ROLE_HIERARCHY);
    }
    
    /**
     * Resolves the primary (highest priority) role from a list
     */
    public static Role resolvePrimaryRole(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Role.USER;
        }
        for (Role role : ROLE_PRIORITY) {
            if (roles.contains(role)) {
                return role;
            }
        }
        return Role.USER;
    }
    
    /**
     * Merges roles, ensuring User is always present if empty
     */
    public static List<Role> mergeRoles(Role primary, List<Role> roles) {
        List<Role> merged = new ArrayList<>();
        if (roles != null) {
            merged.addAll(roles);
        }
        if (primary != null && !merged.contains(primary)) {
            merged.add(primary);
        }
        merged = merged.stream().distinct().toList();
        if (merged.isEmpty()) {
            return new ArrayList<>(List.of(Role.USER));
        }
        return new ArrayList<>(merged);
    }
    
    /**
     * Checks if an actor can assign the target roles based on role hierarchy.
     * Returns true if the actor's highest role is higher or equal to the target's highest role.
     */
    public static boolean canAssignRoles(List<Role> actorRoles, List<Role> targetRoles) {
        if (targetRoles == null || targetRoles.isEmpty()) {
            return false;
        }
        
        List<Role> actorEffective = expandRoles(actorRoles);
        
        // Find actor's highest role index (lower index = higher priority)
        int actorTopIdx = -1;
        for (int i = 0; i < ROLE_PRIORITY.size(); i++) {
            if (actorEffective.contains(ROLE_PRIORITY.get(i))) {
                actorTopIdx = i;
                break;
            }
        }
        if (actorTopIdx == -1) {
            return false;
        }
        
        // Find target's highest role index
        int targetTopIdx = -1;
        for (int i = 0; i < ROLE_PRIORITY.size(); i++) {
            if (targetRoles.contains(ROLE_PRIORITY.get(i))) {
                targetTopIdx = i;
                break;
            }
        }
        if (targetTopIdx == -1) {
            return false;
        }
        
        // Actor can assign if their priority is higher or equal (lower index)
        return actorTopIdx <= targetTopIdx;
    }
}

