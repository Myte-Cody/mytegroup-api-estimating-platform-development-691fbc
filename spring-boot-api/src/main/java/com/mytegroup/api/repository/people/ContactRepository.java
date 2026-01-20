package com.mytegroup.api.repository.people;

import com.mytegroup.api.entity.enums.people.ContactPersonType;
import com.mytegroup.api.entity.people.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // List with pagination
    Page<Contact> findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable);

    // Filter by person type
    List<Contact> findByOrgIdAndPersonTypeAndArchivedAtIsNull(Long orgId, ContactPersonType personType);

    // Find by email
    List<Contact> findByOrgIdAndEmail(Long orgId, String email);

    // Find by ironworker number
    List<Contact> findByOrgIdAndIronworkerNumber(Long orgId, String number);

    // Find by office
    List<Contact> findByOrgIdAndOfficeId(Long orgId, Long officeId);

    // Find direct reports
    List<Contact> findByOrgIdAndReportsToContactId(Long orgId, Long managerId);

    // Find all for org (including archived)
    List<Contact> findByOrgId(Long orgId);

    // Check if active exists
    boolean existsByOrgIdAndArchivedAtIsNull(Long orgId);
}

