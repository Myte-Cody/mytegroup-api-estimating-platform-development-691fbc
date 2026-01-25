package com.mytegroup.api.dto.persons;

import com.mytegroup.api.dto.common.ValidationConstants;
import com.mytegroup.api.entity.enums.people.PersonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

public record CreatePersonDto(
    @NotNull(message = "Person type is required")
    PersonType personType,
    @NotBlank(message = "Display name is required")
    String displayName,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    List<@Email String> emails,
    @Email(message = "Email must be valid")
    String primaryEmail,
    List<@Pattern(regexp = ValidationConstants.PHONE_REGEX) String> phones,
    @Pattern(regexp = ValidationConstants.PHONE_REGEX, message = "Primary phone must be valid")
    String primaryPhone,
    List<String> tagKeys,
    List<String> skillKeys,
    String departmentKey,
    String orgLocationId,
    String reportsToPersonId,
    String ironworkerNumber,
    String unionLocal,
    List<String> skillFreeText,
    @Valid
    List<PersonCertificationDto> certifications,
    Double rating,
    String notes,
    String companyId,
    String companyLocationId,
    String title
) {
    public CreatePersonDto {
        if (displayName != null) {
            displayName = displayName.trim();
        }
        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
        if (primaryEmail != null) {
            primaryEmail = primaryEmail.toLowerCase().trim();
        }
        if (primaryPhone != null) {
            primaryPhone = primaryPhone.trim();
        }
        if (departmentKey != null) {
            departmentKey = departmentKey.trim();
        }
        if (ironworkerNumber != null) {
            ironworkerNumber = ironworkerNumber.trim();
        }
        if (unionLocal != null) {
            unionLocal = unionLocal.trim();
        }
        if (notes != null) {
            notes = notes.trim();
        }
        if (title != null) {
            title = title.trim();
        }
    }
}

