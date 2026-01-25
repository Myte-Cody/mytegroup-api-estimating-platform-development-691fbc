package com.mytegroup.api.service.costcodes;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.cost.CostCodeRepository;
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
    public CostCode create(CostCode costCode, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        Organization org = authHelper.validateOrg(orgId);
        costCode.setOrganization(org);
        
        Long orgIdLong = Long.parseLong(orgId);
        
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
        if (costCodeRepository.findByOrganization_IdAndCode(orgIdLong, code).isPresent()) {
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
            orgId,
            null, // userId will be set when sessions are implemented
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
    public Page<CostCode> list(String orgId, String q, Boolean activeOnly, int page, int limit) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        Long orgIdLong = Long.parseLong(orgId);
        Specification<CostCode> spec = Specification.where(
            (root, query, cb) -> cb.equal(root.get("organization").get("id"), orgIdLong)
        );
        
        if (activeOnly != null && activeOnly) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
        }
        
        if (q != null && !q.trim().isEmpty()) {
            String searchPattern = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("code")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern),
                    cb.like(cb.lower(root.get("category")), searchPattern)
                )
            );
        }
        
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit);
        
        return costCodeRepository.findAll(spec, pageable);
    }
    
    /**
     * Gets a cost code by ID
     */
    @Transactional(readOnly = true)
    public CostCode getById(Long id, String orgId) {
        CostCode costCode = costCodeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cost code not found"));
        
        if (orgId != null && costCode.getOrganization() != null && 
            !costCode.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access cost codes outside your organization");
        }
        
        return costCode;
    }
    
    /**
     * Updates a cost code
     */
    @Transactional
    public CostCode update(Long id, CostCode costCodeUpdates, String orgId) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        CostCode costCode = costCodeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cost code not found"));
        
        if (costCode.getOrganization() != null && 
            !costCode.getOrganization().getId().toString().equals(orgId)) {
            throw new ForbiddenException("Cannot access cost codes outside your organization");
        }
        
        Long orgIdLong = costCode.getOrganization().getId();
        
        // Update code with uniqueness check
        if (costCodeUpdates.getCode() != null && !costCodeUpdates.getCode().equals(costCode.getCode())) {
            String newCode = costCodeUpdates.getCode().trim();
            if (costCodeRepository.findByOrganization_IdAndCode(orgIdLong, newCode)
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
            orgId,
            null, // userId will be set when sessions are implemented
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
    public CostCode toggleActive(Long id, boolean active, String orgId) {
        CostCode costCode = getById(id, orgId);
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
            orgId,
            null, // userId will be set when sessions are implemented
            "CostCode",
            savedCostCode.getId().toString(),
            metadata
        );
        
        return savedCostCode;
    }
}

