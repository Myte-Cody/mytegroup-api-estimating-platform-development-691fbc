package com.mytegroup.api.service.legal;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.legal.LegalDoc;
import com.mytegroup.api.entity.legal.LegalAcceptance;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.legal.LegalDocRepository;
import com.mytegroup.api.repository.legal.LegalAcceptanceRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for legal document management.
 * Handles legal documents (Privacy Policy, Terms & Conditions) and user acceptances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegalService {
    
    private final LegalDocRepository legalDocRepository;
    private final LegalAcceptanceRepository legalAcceptanceRepository;
    private final AuditLogService auditLogService;
    private final ServiceAuthorizationHelper authHelper;
    
    /**
     * Gets the latest legal document by type
     */
    @Transactional(readOnly = true)
    public LegalDoc getLatest(com.mytegroup.api.entity.enums.legal.LegalDocType type) {
        return legalDocRepository.findByTypeOrderByEffectiveAtDescCreatedAtDesc(type)
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Legal document not found"));
    }
    
    /**
     * Creates a new legal document
     */
    @Transactional
    public LegalDoc createDoc(LegalDoc doc) {
        // Role validation handled by Spring Security @PreAuthorize on controller
        
        // Check for version collision
        if (legalDocRepository.findByTypeAndVersion(doc.getType(), doc.getVersion()).isPresent()) {
            throw new ConflictException("Legal document version already exists");
        }
        
        LegalDoc savedDoc = legalDocRepository.save(doc);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", savedDoc.getType());
        metadata.put("version", savedDoc.getVersion());
        
        auditLogService.log(
            "legal.doc_created",
            null,
            null,
            "LegalDoc",
            savedDoc.getId().toString(),
            metadata
        );
        
        return savedDoc;
    }
    
    /**
     * Records user acceptance of a legal document
     */
    @Transactional
    public LegalAcceptance accept(String type, String version) {
        if (null == null) {
            throw new BadRequestException("User context required");
        }
        
        com.mytegroup.api.entity.enums.legal.LegalDocType docType = 
            com.mytegroup.api.entity.enums.legal.LegalDocType.valueOf(type.toUpperCase());
        LegalDoc doc = legalDocRepository.findByTypeAndVersion(docType, version)
            .orElseThrow(() -> new ResourceNotFoundException("Legal document not found"));
        
        // Check if already accepted
        Long userId = Long.parseLong(null);
        if (legalAcceptanceRepository.findByUserIdAndDocTypeAndVersion(userId, docType, version).isPresent()) {
            throw new BadRequestException("Legal document already accepted");
        }
        
        LegalAcceptance acceptance = new LegalAcceptance();
        // TODO: Set user from actor
        // acceptance.setUser(user);
        acceptance.setDocType(docType);
        acceptance.setVersion(version);
        if (null != null) {
            // TODO: Set organization from actor
            // acceptance.setOrganization(org);
        }
        acceptance.setAcceptedAt(LocalDateTime.now());
        
        LegalAcceptance savedAcceptance = legalAcceptanceRepository.save(acceptance);
        
        auditLogService.log(
            "legal.accepted",
            null,
            null,
            "LegalAcceptance",
            savedAcceptance.getId().toString(),
            Map.of("type", type, "version", version)
        );
        
        return savedAcceptance;
    }
    
    /**
     * Lists legal documents
     */
    @Transactional(readOnly = true)
    public List<LegalDoc> list(String type) {
        if (type != null && !type.trim().isEmpty()) {
            com.mytegroup.api.entity.enums.legal.LegalDocType docType = 
                com.mytegroup.api.entity.enums.legal.LegalDocType.valueOf(type.toUpperCase());
            return legalDocRepository.findByTypeOrderByEffectiveAtDescCreatedAtDesc(docType);
        }
        // TODO: Implement list all - requires custom query
        return List.of();
    }
}

