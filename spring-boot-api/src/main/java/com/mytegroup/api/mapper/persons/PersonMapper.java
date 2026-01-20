package com.mytegroup.api.mapper.persons;

import com.mytegroup.api.dto.persons.CreatePersonDto;
import com.mytegroup.api.dto.persons.PersonCertificationDto;
import com.mytegroup.api.dto.persons.UpdatePersonDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.people.embeddable.PersonCertification;
import com.mytegroup.api.entity.people.embeddable.PersonEmail;
import com.mytegroup.api.entity.people.embeddable.PersonPhone;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PersonMapper {

    /**
     * Maps CreatePersonDto to Person entity.
     */
    public Person toEntity(CreatePersonDto dto, Organization organization, Office orgLocation, 
                          Company company, CompanyLocation companyLocation, Person reportsTo) {
        Person person = new Person();
        person.setOrganization(organization);
        person.setPersonType(dto.personType());
        person.setDisplayName(dto.displayName());
        person.setFirstName(dto.firstName());
        person.setLastName(dto.lastName());
        person.setDateOfBirth(dto.dateOfBirth());
        person.setPrimaryEmail(dto.primaryEmail());
        person.setPrimaryPhoneE164(dto.primaryPhone());
        person.setTagKeys(dto.tagKeys() != null ? new ArrayList<>(dto.tagKeys()) : new ArrayList<>());
        person.setSkillKeys(dto.skillKeys() != null ? new ArrayList<>(dto.skillKeys()) : new ArrayList<>());
        person.setDepartmentKey(dto.departmentKey());
        person.setOrgLocation(orgLocation);
        person.setReportsTo(reportsTo);
        person.setCompany(company);
        person.setCompanyLocation(companyLocation);
        person.setIronworkerNumber(dto.ironworkerNumber());
        person.setUnionLocal(dto.unionLocal());
        person.setSkillFreeText(dto.skillFreeText() != null ? new ArrayList<>(dto.skillFreeText()) : new ArrayList<>());
        person.setRating(dto.rating());
        person.setNotes(dto.notes());
        person.setTitle(dto.title());
        
        // Map emails
        if (dto.emails() != null && !dto.emails().isEmpty()) {
            List<PersonEmail> personEmails = dto.emails().stream()
                .map(email -> {
                    PersonEmail pe = new PersonEmail();
                    pe.setValue(email);
                    pe.setNormalized(email.toLowerCase().trim());
                    pe.setIsPrimary(email.equals(dto.primaryEmail()));
                    return pe;
                })
                .collect(Collectors.toList());
            person.setEmails(personEmails);
        }
        
        // Map phones
        if (dto.phones() != null && !dto.phones().isEmpty()) {
            List<PersonPhone> personPhones = dto.phones().stream()
                .map(phone -> {
                    PersonPhone pp = new PersonPhone();
                    pp.setValue(phone);
                    pp.setE164(phone); // TODO: Normalize to E.164 format
                    pp.setIsPrimary(phone.equals(dto.primaryPhone()));
                    return pp;
                })
                .collect(Collectors.toList());
            person.setPhones(personPhones);
        }
        
        // Map certifications
        if (dto.certifications() != null && !dto.certifications().isEmpty()) {
            List<PersonCertification> certs = dto.certifications().stream()
                .map(this::toCertificationEntity)
                .collect(Collectors.toList());
            person.setCertifications(certs);
        }
        
        return person;
    }

    /**
     * Updates existing Person entity with UpdatePersonDto values.
     */
    public void updateEntity(Person person, UpdatePersonDto dto, Office orgLocation,
                           Company company, CompanyLocation companyLocation, Person reportsTo) {
        if (dto.personType() != null) {
            person.setPersonType(dto.personType());
        }
        if (dto.displayName() != null) {
            person.setDisplayName(dto.displayName());
        }
        if (dto.firstName() != null) {
            person.setFirstName(dto.firstName());
        }
        if (dto.lastName() != null) {
            person.setLastName(dto.lastName());
        }
        if (dto.dateOfBirth() != null) {
            person.setDateOfBirth(dto.dateOfBirth());
        }
        if (dto.primaryEmail() != null) {
            person.setPrimaryEmail(dto.primaryEmail());
        }
        if (dto.primaryPhone() != null) {
            person.setPrimaryPhoneE164(dto.primaryPhone());
        }
        if (dto.tagKeys() != null) {
            person.setTagKeys(new ArrayList<>(dto.tagKeys()));
        }
        if (dto.skillKeys() != null) {
            person.setSkillKeys(new ArrayList<>(dto.skillKeys()));
        }
        if (dto.departmentKey() != null) {
            person.setDepartmentKey(dto.departmentKey());
        }
        if (orgLocation != null) {
            person.setOrgLocation(orgLocation);
        }
        if (reportsTo != null) {
            person.setReportsTo(reportsTo);
        }
        if (company != null) {
            person.setCompany(company);
        }
        if (companyLocation != null) {
            person.setCompanyLocation(companyLocation);
        }
        if (dto.ironworkerNumber() != null) {
            person.setIronworkerNumber(dto.ironworkerNumber());
        }
        if (dto.unionLocal() != null) {
            person.setUnionLocal(dto.unionLocal());
        }
        if (dto.skillFreeText() != null) {
            person.setSkillFreeText(new ArrayList<>(dto.skillFreeText()));
        }
        if (dto.rating() != null) {
            person.setRating(dto.rating());
        }
        if (dto.notes() != null) {
            person.setNotes(dto.notes());
        }
        if (dto.title() != null) {
            person.setTitle(dto.title());
        }
        
        // Update emails if provided
        if (dto.emails() != null) {
            List<PersonEmail> personEmails = dto.emails().stream()
                .map(email -> {
                    PersonEmail pe = new PersonEmail();
                    pe.setValue(email);
                    pe.setNormalized(email.toLowerCase().trim());
                    pe.setIsPrimary(email.equals(dto.primaryEmail()));
                    return pe;
                })
                .collect(Collectors.toList());
            person.setEmails(personEmails);
        }
        
        // Update phones if provided
        if (dto.phones() != null) {
            List<PersonPhone> personPhones = dto.phones().stream()
                .map(phone -> {
                    PersonPhone pp = new PersonPhone();
                    pp.setValue(phone);
                    pp.setE164(phone); // TODO: Normalize to E.164 format
                    pp.setIsPrimary(phone.equals(dto.primaryPhone()));
                    return pp;
                })
                .collect(Collectors.toList());
            person.setPhones(personPhones);
        }
        
        // Update certifications if provided
        if (dto.certifications() != null) {
            List<PersonCertification> certs = dto.certifications().stream()
                .map(this::toCertificationEntity)
                .collect(Collectors.toList());
            person.setCertifications(certs);
        }
    }

    private PersonCertification toCertificationEntity(PersonCertificationDto dto) {
        PersonCertification cert = new PersonCertification();
        cert.setName(dto.name());
        cert.setIssuedAt(dto.issuedAt() != null ? dto.issuedAt().atStartOfDay() : null);
        cert.setExpiresAt(dto.expiresAt() != null ? dto.expiresAt().atStartOfDay() : null);
        cert.setDocumentUrl(dto.documentUrl());
        cert.setNotes(dto.notes());
        return cert;
    }
}

