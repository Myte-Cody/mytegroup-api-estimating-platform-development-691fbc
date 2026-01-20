package com.mytegroup.api.repository.communication;

import com.mytegroup.api.entity.communication.ContactInquiry;
import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {

    // Find by status ordered by created date desc
    Page<ContactInquiry> findByStatusOrderByCreatedAtDesc(ContactInquiryStatus status, Pageable pageable);

    // List all ordered
    Page<ContactInquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

