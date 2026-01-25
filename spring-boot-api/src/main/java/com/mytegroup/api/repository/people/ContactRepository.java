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
    Page<Contact> findByOrganization_IdAndArchivedAtIsNull(Long organizationId, Pageable pageable);

    // Filter by person type
    List<Contact> findByOrganization_IdAndPersonTypeAndArchivedAtIsNull(Long organizationId, ContactPersonType personType);

    // Find by email
    List<Contact> findByOrganization_IdAndEmail(Long organizationId, String email);

    // Find by ironworker number
    List<Contact> findByOrganization_IdAndIronworkerNumber(Long organizationId, String number);

    // Find by office
    List<Contact> findByOrganization_IdAndOfficeId(Long organizationId, Long officeId);

    // Find direct reports
    List<Contact> findByOrganization_IdAndReportsToId(Long organizationId, Long managerId);

    // Find all for org (including archived)
    List<Contact> findByOrganization_Id(Long organizationId);

    // Check if active exists
    boolean existsByOrganization_IdAndArchivedAtIsNull(Long organizationId);
}

