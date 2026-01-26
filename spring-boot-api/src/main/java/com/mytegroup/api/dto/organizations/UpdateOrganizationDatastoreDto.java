package com.mytegroup.api.dto.organizations;

import com.mytegroup.api.entity.enums.organization.DataResidency;
import com.mytegroup.api.entity.enums.organization.DatastoreType;

public record UpdateOrganizationDatastoreDto(
    Boolean useDedicatedDb,
    DatastoreType type,
    String databaseUri,
    String datastoreUri,
    String databaseName,
    DataResidency dataResidency
) {
}



