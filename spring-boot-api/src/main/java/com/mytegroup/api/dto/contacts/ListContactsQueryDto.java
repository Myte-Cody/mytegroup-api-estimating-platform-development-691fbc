package com.mytegroup.api.dto.contacts;

import com.mytegroup.api.entity.enums.people.PersonType;

public record ListContactsQueryDto(
    String orgId,
    Boolean includeArchived,
    PersonType personType
) {
    public ListContactsQueryDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
    }
}
