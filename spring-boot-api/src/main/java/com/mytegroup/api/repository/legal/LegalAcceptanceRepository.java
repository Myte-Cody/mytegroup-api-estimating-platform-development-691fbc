package com.mytegroup.api.repository.legal;

import com.mytegroup.api.entity.enums.legal.LegalDocType;
import com.mytegroup.api.entity.legal.LegalAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptance, Long> {

    // Find acceptance (unique)
    Optional<LegalAcceptance> findByUserIdAndDocTypeAndVersion(Long userId, LegalDocType docType, String version);

    // Find all acceptances for doc type
    List<LegalAcceptance> findByUserIdAndDocType(Long userId, LegalDocType docType);

    // Find by org
    List<LegalAcceptance> findByOrgIdAndDocTypeAndVersion(Long orgId, LegalDocType docType, String version);
}

