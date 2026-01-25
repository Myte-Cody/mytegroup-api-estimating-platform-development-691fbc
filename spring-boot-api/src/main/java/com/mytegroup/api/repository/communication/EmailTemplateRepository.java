package com.mytegroup.api.repository.communication;

import com.mytegroup.api.entity.communication.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    // Find by name and locale (unique)
    Optional<EmailTemplate> findByOrganization_IdAndNameAndLocale(Long organizationId, String name, String locale);

    // Find all locales for template
    List<EmailTemplate> findByOrganization_IdAndName(Long organizationId, String name);

    // Find all for org
    List<EmailTemplate> findByOrganization_Id(Long organizationId);
}

