package com.mytegroup.api.repository.legal;

import com.mytegroup.api.entity.enums.legal.LegalDocType;
import com.mytegroup.api.entity.legal.LegalDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalDocRepository extends JpaRepository<LegalDoc, Long> {

    // Find by type and version (unique)
    Optional<LegalDoc> findByTypeAndVersion(LegalDocType type, String version);

    // Find latest by type
    @Query("SELECT ld FROM LegalDoc ld WHERE ld.type = :type AND ld.archivedAt IS NULL ORDER BY ld.effectiveAt DESC, ld.createdAt DESC")
    List<LegalDoc> findByTypeOrderByEffectiveAtDescCreatedAtDesc(@Param("type") LegalDocType type);
}

