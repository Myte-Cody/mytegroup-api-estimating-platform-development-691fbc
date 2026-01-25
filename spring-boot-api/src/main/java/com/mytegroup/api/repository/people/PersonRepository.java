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
    Page<Person> findByOrganization_IdAndArchivedAtIsNull(Long organizationId, Pageable pageable);

    // Filter by type
    List<Person> findByOrganization_IdAndPersonTypeAndArchivedAtIsNull(Long organizationId, PersonType personType);

    // Find by primary email
    Optional<Person> findByOrganization_IdAndPrimaryEmail(Long organizationId, String email);

    // Find by phone
    Optional<Person> findByOrganization_IdAndPrimaryPhoneE164(Long organizationId, String phone);

    // Find by ironworker number
    Optional<Person> findByOrganization_IdAndIronworkerNumber(Long organizationId, String number);

    // Find by company
    List<Person> findByOrganization_IdAndCompanyId(Long organizationId, Long companyId);

    // Find by location
    List<Person> findByOrganization_IdAndCompanyLocationId(Long organizationId, Long locationId);

    // Find by office
    List<Person> findByOrganization_IdAndOrgLocationId(Long organizationId, Long officeId);

    // Find direct reports
    List<Person> findByOrganization_IdAndReportsToId(Long organizationId, Long managerId);

    // Search by name
    List<Person> findByOrganization_IdAndDisplayNameContainingIgnoreCase(Long organizationId, String search);

    // Find by tag
    @Query("SELECT DISTINCT p FROM Person p JOIN p.tagKeys t WHERE p.organization.id = :orgId AND t = :tagKey AND p.archivedAt IS NULL")
    List<Person> findByOrganization_IdAndTagKeysContaining(@Param("orgId") Long orgId, @Param("tagKey") String tagKey);

    // Find by skill
    @Query("SELECT DISTINCT p FROM Person p JOIN p.skillKeys s WHERE p.organization.id = :orgId AND s = :skillKey AND p.archivedAt IS NULL")
    List<Person> findByOrganization_IdAndSkillKeysContaining(@Param("orgId") Long orgId, @Param("skillKey") String skillKey);

    // Complex search by email normalized
    @Query("SELECT DISTINCT p FROM Person p JOIN p.emails e WHERE p.organization.id = :orgId AND e.normalized = :email AND p.archivedAt IS NULL")
    Optional<Person> findByOrganization_IdAndEmailNormalized(@Param("orgId") Long orgId, @Param("email") String email);

    // Complex search by phone e164
    @Query("SELECT DISTINCT p FROM Person p JOIN p.phones ph WHERE p.organization.id = :orgId AND ph.e164 = :phone AND p.archivedAt IS NULL")
    Optional<Person> findByOrganization_IdAndPhoneE164(@Param("orgId") Long orgId, @Param("phone") String phone);

    // Find all for org (including archived)
    List<Person> findByOrganization_Id(Long organizationId);

    // Check if active exists
    boolean existsByOrganization_IdAndArchivedAtIsNull(Long organizationId);
}

