package com.mytegroup.api.service.costcodes;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.cost.CostCodeRepository;
import com.mytegroup.api.service.common.ActorContext;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for cost code management.
 * Handles CRUD operations, activation/deactivation, and import jobs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CostCodesService {
    
    private final CostCodeRepository costCodeRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    
    /**
     * Creates a new cost code
     */
    @Transactional
    public CostCode create(CostCode costCode, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN, Role.ADMIN, Role.ORG_OWNER, Role.ORG_ADMIN);
        
        if (orgId == null && actor.getOrgId() == null) {
            throw new BadRequestException("Missing organization context");
        }
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        authHelper.ensureOrgScope(resolvedOrgId, actor);
        Organization org = authHelper.validateOrg(resolvedOrgId);
        costCode.setOrganization(org);
        
        Long orgIdLong = Long.parseLong(resolvedOrgId);
        
        // Validate required fields
        if (costCode.getCategory() == null || costCode.getCategory().trim().isEmpty()) {
            throw new BadRequestException("Category is required");
        }
        if (costCode.getCode() == null || costCode.getCode().trim().isEmpty()) {
            throw new BadRequestException("Code is required");
        }
        if (costCode.getDescription() == null || costCode.getDescription().trim().isEmpty()) {
            throw new BadRequestException("Description is required");
        }
        
        // Check code uniqueness
        String code = costCode.getCode().trim();
        if (costCodeRepository.findByOrgIdAndCode(orgIdLong, code).isPresent()) {
            throw new ConflictException("Cost code already exists for this organization");
        }
        costCode.setCode(code);
        
        // Set defaults
        if (costCode.getActive() == null) {
            costCode.setActive(false);
        }
        if (costCode.getIsUsed() == null) {
            costCode.setIsUsed(false);
        }
        
        CostCode savedCostCode = costCodeRepository.save(costCode);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("code", savedCostCode.getCode());
        metadata.put("category", savedCostCode.getCategory());
        
        auditLogService.log(
            "cost_code.created",
            resolvedOrgId,
            actor != null ? actor.getUserId() : null,
            "CostCode",
            savedCostCode.getId().toString(),
            metadata
        );
        
        return savedCostCode;
    }
    
    /**
     * Lists cost codes for an organization
     */
    @Transactional(readOnly = true)
    public List<CostCode> list(ActorContext actor, String orgId, Boolean active, String category, String q) {
        authHelper.ensureRole(actor, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN, Role.ADMIN, Role.ORG_OWNER, Role.ORG_ADMIN);
        
        if (orgId == null && actor.getOrgId() == null) {
            throw new BadRequestException("Missing organization context");
        }
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        authHelper.ensureOrgScope(resolvedOrgId, actor);
        
        Long orgIdLong = Long.parseLong(resolvedOrgId);
        
        if (active != null) {
            return costCodeRepository.findByOrgIdAndActive(orgIdLong, active);
        }
        if (category != null && !category.trim().isEmpty()) {
            return costCodeRepository.findByOrgIdAndCategory(orgIdLong, category);
        }
        
        // TODO: Implement search query (q parameter) - requires Specification or custom query
        return costCodeRepository.findByOrgId(orgIdLong);
    }
    
    /**
     * Gets a cost code by ID
     */
    @Transactional(readOnly = true)
    public CostCode getById(Long id, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN, Role.ADMIN, Role.ORG_OWNER, Role.ORG_ADMIN);
        
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        if (resolvedOrgId != null) {
            authHelper.ensureOrgScope(resolvedOrgId, actor);
        }
        
        CostCode costCode = costCodeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cost code not found"));
        
        if (resolvedOrgId != null && costCode.getOrganization() != null && 
            !costCode.getOrganization().getId().toString().equals(resolvedOrgId)) {
            throw new ForbiddenException("Cannot access cost codes outside your organization");
        }
        
        return costCode;
    }
    
    /**
     * Updates a cost code
     */
    @Transactional
    public CostCode update(Long id, CostCode costCodeUpdates, ActorContext actor, String orgId) {
        authHelper.ensureRole(actor, Role.SUPER_ADMIN, Role.PLATFORM_ADMIN, Role.ADMIN, Role.ORG_OWNER, Role.ORG_ADMIN);
        
        String resolvedOrgId = orgId != null ? orgId : actor.getOrgId();
        if (resolvedOrgId != null) {
            authHelper.ensureOrgScope(resolvedOrgId, actor);
        }
        
        CostCode costCode = costCodeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cost code not found"));
        
        if (resolvedOrgId != null && costCode.getOrganization() != null && 
            !costCode.getOrganization().getId().toString().equals(resolvedOrgId)) {
            throw new ForbiddenException("Cannot access cost codes outside your organization");
        }
        
        Long orgIdLong = costCode.getOrganization().getId();
        
        // Update code with uniqueness check
        if (costCodeUpdates.getCode() != null && !costCodeUpdates.getCode().equals(costCode.getCode())) {
            String newCode = costCodeUpdates.getCode().trim();
            if (costCodeRepository.findByOrgIdAndCode(orgIdLong, newCode)
                .filter(c -> !c.getId().equals(id))
                .isPresent()) {
                throw new ConflictException("Cost code already exists for this organization");
            }
            costCode.setCode(newCode);
        }
        
        // Update other fields
        if (costCodeUpdates.getCategory() != null) {
            costCode.setCategory(costCodeUpdates.getCategory());
        }
        if (costCodeUpdates.getDescription() != null) {
            costCode.setDescription(costCodeUpdates.getDescription());
        }
        if (costCodeUpdates.getActive() != null) {
            costCode.setActive(costCodeUpdates.getActive());
            if (!costCodeUpdates.getActive()) {
                costCode.setDeactivatedAt(LocalDateTime.now());
            } else {
                costCode.setDeactivatedAt(null);
            }
        }
        
        CostCode savedCostCode = costCodeRepository.save(costCode);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changes", Map.of("updated", true));
        
        auditLogService.log(
            "cost_code.updated",
            resolvedOrgId != null ? resolvedOrgId : savedCostCode.getOrganization().getId().toString(),
            actor != null ? actor.getUserId() : null,
            "CostCode",
            savedCostCode.getId().toString(),
            metadata
        );
        
        return savedCostCode;
    }
    
    /**
     * Toggles cost code active status
     */
    @Transactional
    public CostCode toggleActive(Long id, boolean active, ActorContext actor, String orgId) {
        CostCode costCode = getById(id, actor, orgId);
        costCode.setActive(active);
        if (!active) {
            costCode.setDeactivatedAt(LocalDateTime.now());
        } else {
            costCode.setDeactivatedAt(null);
        }
        
        CostCode savedCostCode = costCodeRepository.save(costCode);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("active", savedCostCode.getActive());
        
        auditLogService.log(
            "cost_code.toggled",
            orgId != null ? orgId : savedCostCode.getOrganization().getId().toString(),
            actor != null ? actor.getUserId() : null,
            "CostCode",
            savedCostCode.getId().toString(),
            metadata
        );
        
        return savedCostCode;
    }
}

