package com.mytegroup.api.entity.enums.people;

/**
 * Person type enum.
 * Used in Person entity.
 */
public enum PersonType {
    INTERNAL_STAFF("internal_staff"),
    INTERNAL_UNION("internal_union"),
    EXTERNAL_PERSON("external_person");

    private final String value;

    PersonType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PersonType fromValue(String value) {
        for (PersonType type : PersonType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown person type value: " + value);
    }
}

