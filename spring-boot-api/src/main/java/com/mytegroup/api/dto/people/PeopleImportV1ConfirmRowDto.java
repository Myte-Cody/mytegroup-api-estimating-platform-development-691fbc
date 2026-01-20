package com.mytegroup.api.dto.people;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.common.ValidationConstants;
import com.mytegroup.api.entity.enums.people.PersonType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PeopleImportV1ConfirmRowDto(
    @NotNull(message = "Row number is required")
    @Min(value = 1, message = "Row number must be at least 1")
    Integer row,
    @NotNull(message = "Person type is required")
    PersonType personType,
    @NotBlank(message = "Display name is required")
    String displayName,
    @Size(max = 10, message = "Emails must not exceed 10 items")
    @Email(message = "Email must be valid")
    List<@Email String> emails,
    @Email(message = "Email must be valid")
    String primaryEmail,
    @Size(max = 10, message = "Phones must not exceed 10 items")
    @Pattern(regexp = ValidationConstants.PHONE_REGEX, message = "Phone must be valid")
    List<@Pattern(regexp = ValidationConstants.PHONE_REGEX) String> phones,
    @Pattern(regexp = ValidationConstants.PHONE_REGEX, message = "Primary phone must be valid")
    String primaryPhone,
    @Size(max = 100, message = "Tag keys must not exceed 100 items")
    List<String> tagKeys,
    @Size(max = 100, message = "Skill keys must not exceed 100 items")
    List<String> skillKeys,
    String departmentKey,
    String title,
    String orgLocationName,
    String reportsToDisplayName,
    String companyExternalId,
    String companyName,
    String companyLocationExternalId,
    String companyLocationName,
    String ironworkerNumber,
    String unionLocal,
    @Size(max = 100, message = "Certifications must not exceed 100 items")
    List<String> certifications,
    Double rating,
    String notes,
    Role inviteRole,
    @NotNull(message = "Action is required")
    String action,
    String personId
) {
    public PeopleImportV1ConfirmRowDto {
        if (displayName != null) {
            displayName = displayName.trim();
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
        if (title != null) {
            title = title.trim();
        }
        if (orgLocationName != null) {
            orgLocationName = orgLocationName.trim();
        }
        if (reportsToDisplayName != null) {
            reportsToDisplayName = reportsToDisplayName.trim();
        }
        if (companyExternalId != null) {
            companyExternalId = companyExternalId.trim();
        }
        if (companyName != null) {
            companyName = companyName.trim();
        }
        if (companyLocationExternalId != null) {
            companyLocationExternalId = companyLocationExternalId.trim();
        }
        if (companyLocationName != null) {
            companyLocationName = companyLocationName.trim();
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

