package com.mytegroup.api.entity.enums.people;

/**
 * Contact person type enum.
 * Different from PersonType - separate enum for Contact entity.
 */
public enum ContactPersonType {
    STAFF("staff"),
    IRONWORKER("ironworker"),
    EXTERNAL("external");

    private final String value;

    ContactPersonType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ContactPersonType fromValue(String value) {
        for (ContactPersonType type : ContactPersonType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown contact person type value: " + value);
    }
}

