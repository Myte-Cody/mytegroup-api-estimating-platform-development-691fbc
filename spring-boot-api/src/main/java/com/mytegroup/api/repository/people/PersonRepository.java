package com.mytegroup.api.repository.people;

import com.mytegroup.api.entity.enums.people.PersonType;
import com.mytegroup.api.entity.people.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    // List with pagination
    Page<Person> findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable);

    // Filter by type
    List<Person> findByOrgIdAndPersonTypeAndArchivedAtIsNull(Long orgId, PersonType personType);

    // Find by primary email
    Optional<Person> findByOrgIdAndPrimaryEmail(Long orgId, String email);

    // Find by phone
    Optional<Person> findByOrgIdAndPrimaryPhoneE164(Long orgId, String phone);

    // Find by ironworker number
    Optional<Person> findByOrgIdAndIronworkerNumber(Long orgId, String number);

    // Find by company
    List<Person> findByOrgIdAndCompanyId(Long orgId, Long companyId);

    // Find by location
    List<Person> findByOrgIdAndCompanyLocationId(Long orgId, Long locationId);

    // Find by office
    List<Person> findByOrgIdAndOrgLocationId(Long orgId, Long officeId);

    // Find direct reports
    List<Person> findByOrgIdAndReportsToPersonId(Long orgId, Long managerId);

    // Search by name
    List<Person> findByOrgIdAndDisplayNameContainingIgnoreCase(Long orgId, String search);

    // Find by tag
    @Query("SELECT DISTINCT p FROM Person p JOIN p.tagKeys t WHERE p.orgId = :orgId AND t = :tagKey AND p.archivedAt IS NULL")
    List<Person> findByOrgIdAndTagKeysContaining(@Param("orgId") Long orgId, @Param("tagKey") String tagKey);

    // Find by skill
    @Query("SELECT DISTINCT p FROM Person p JOIN p.skillKeys s WHERE p.orgId = :orgId AND s = :skillKey AND p.archivedAt IS NULL")
    List<Person> findByOrgIdAndSkillKeysContaining(@Param("orgId") Long orgId, @Param("skillKey") String skillKey);

    // Complex search by email normalized
    @Query("SELECT DISTINCT p FROM Person p JOIN p.emails e WHERE p.orgId = :orgId AND e.normalized = :email AND p.archivedAt IS NULL")
    Optional<Person> findByOrgIdAndEmailNormalized(@Param("orgId") Long orgId, @Param("email") String email);

    // Complex search by phone e164
    @Query("SELECT DISTINCT p FROM Person p JOIN p.phones ph WHERE p.orgId = :orgId AND ph.e164 = :phone AND p.archivedAt IS NULL")
    Optional<Person> findByOrgIdAndPhoneE164(@Param("orgId") Long orgId, @Param("phone") String phone);

    // Find all for org (including archived)
    List<Person> findByOrgId(Long orgId);

    // Check if active exists
    boolean existsByOrgIdAndArchivedAtIsNull(Long orgId);
}

