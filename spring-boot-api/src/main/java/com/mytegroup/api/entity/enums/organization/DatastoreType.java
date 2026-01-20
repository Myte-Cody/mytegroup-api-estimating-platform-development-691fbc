package com.mytegroup.api.entity.enums.organization;

/**
 * Datastore type enum.
 * Used in Organization entity.
 */
public enum DatastoreType {
    SHARED("shared"),
    DEDICATED("dedicated");

    private final String value;

    DatastoreType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DatastoreType fromValue(String value) {
        for (DatastoreType type : DatastoreType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown datastore type value: " + value);
    }
}

