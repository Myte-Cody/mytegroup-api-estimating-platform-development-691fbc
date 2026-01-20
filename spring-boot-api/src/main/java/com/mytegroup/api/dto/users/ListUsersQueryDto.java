package com.mytegroup.api.dto.users;

public record ListUsersQueryDto(
    Boolean includeArchived,
    String orgId
) {
    public ListUsersQueryDto {
        if (orgId != null) {
            orgId = orgId.trim();
        }
    }
}

