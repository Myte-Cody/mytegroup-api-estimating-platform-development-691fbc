package com.mytegroup.api.service.orgtaxonomy;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
import com.mytegroup.api.entity.organization.embeddable.OrgTaxonomyValue;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.organization.OrgTaxonomyRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for organization taxonomy management.
 * Handles custom taxonomies (tags, types, etc.) for organizations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrgTaxonomyService {
    
    private final OrgTaxonomyRepository orgTaxonomyRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    private final ServiceValidationHelper validationHelper;
    
    /**
     * Gets taxonomy for a namespace
     */
    @Transactional(readOnly = true)
    public OrgTaxonomy getTaxonomy(String orgId, String namespace) {
        String normalizedNamespace = validationHelper.normalizeKey(namespace);
        if (normalizedNamespace == null || normalizedNamespace.isEmpty()) {
            throw new BadRequestException("namespace is required");
        }
        
        Long orgIdLong = Long.parseLong(orgId);
        return orgTaxonomyRepository.findByOrgIdAndNamespace(orgIdLong, normalizedNamespace)
            .orElseThrow(() -> new ResourceNotFoundException("Taxonomy not found"));
    }
    
    /**
     * Creates or updates taxonomy values
     */
    @Transactional
    public OrgTaxonomy putValues(String orgId, String namespace, List<OrgTaxonomyValue> values) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        Organization org = authHelper.validateOrg(orgId);
        
        String normalizedNamespace = validationHelper.normalizeKey(namespace);
        if (normalizedNamespace == null || normalizedNamespace.isEmpty()) {
            throw new BadRequestException("namespace is required");
        }
        
        Long orgIdLong = Long.parseLong(orgId);
        OrgTaxonomy taxonomy = orgTaxonomyRepository.findByOrgIdAndNamespace(orgIdLong, normalizedNamespace)
            .orElseGet(() -> {
                OrgTaxonomy newTaxonomy = new OrgTaxonomy();
                newTaxonomy.setOrganization(org);
                newTaxonomy.setNamespace(normalizedNamespace);
                return newTaxonomy;
            });
        
        // Update values
        taxonomy.setValues(values != null ? values : List.of());
        
        OrgTaxonomy savedTaxonomy = orgTaxonomyRepository.save(taxonomy);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("namespace", normalizedNamespace);
        metadata.put("valueCount", values != null ? values.size() : 0);
        
        auditLogService.log(
            "org_taxonomy.updated",
            orgId,
            null, // userId will be set when sessions are implemented
            "OrgTaxonomy",
            savedTaxonomy.getId().toString(),
            metadata
        );
        
        return savedTaxonomy;
    }
    
    /**
     * Ensures taxonomy keys are active
     */
    @Transactional
    public void ensureKeysActive(String orgId, String namespace, List<String> keys) {
        if (orgId == null) {
            throw new BadRequestException("orgId is required");
        }
        authHelper.validateOrg(orgId);
        
        OrgTaxonomy taxonomy = getTaxonomy(orgId, namespace);
        if (taxonomy == null) {
            throw new ResourceNotFoundException("Taxonomy namespace not found");
        }
        
        // TODO: Validate that all keys exist in taxonomy values
        // This requires checking taxonomy.getValues() for each key
    }
}

