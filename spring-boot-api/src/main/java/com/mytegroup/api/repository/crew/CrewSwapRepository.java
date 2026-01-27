package com.mytegroup.api.repository.crew;

import com.mytegroup.api.entity.crew.CrewSwap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CrewSwapRepository extends JpaRepository<CrewSwap, Long>, JpaSpecificationExecutor<CrewSwap> {
    Page<CrewSwap> findByOrganization_IdAndArchivedAtIsNull(Long orgId, Pageable pageable);
}
