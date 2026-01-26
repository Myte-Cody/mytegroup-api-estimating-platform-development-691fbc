package com.mytegroup.api.dto.people;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.common.ValidationConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

public record PeopleImportConfirmRowDto(
    @NotNull(message = "Row number is required")
    @Min(value = 1, message = "Row number must be at least 1")
    Integer row,
    String personType,
    @NotBlank(message = "Name is required")
    String name,
    @Email(message = "Email must be valid")
    String email,
    @Pattern(regexp = ValidationConstants.PHONE_REGEX, message = "Phone must be valid")
    String phone,
    String company,
    String ironworkerNumber,
    String unionLocal,
    LocalDate dateOfBirth,
    List<String> skills,
    @Valid
    List<ContactCertificationDto> certifications,
    String notes,
    Role inviteRole,
    @NotNull(message = "Action is required")
    String action,
    String personId,
    String contactId
) {
    public PeopleImportConfirmRowDto {
        if (personType != null) {
            personType = personType.trim().toLowerCase();
        }
        if (name != null) {
            name = name.trim();
        }
        if (email != null) {
            email = email.toLowerCase().trim();
        }
        if (phone != null) {
            phone = phone.trim();
        }
        if (company != null) {
            company = company.trim();
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
        if (action != null) {
            action = action.trim().toLowerCase();
        }
    }
}



