package com.mytegroup.api.entity.enums.people;

/**
 * Contact kind enum.
 * Used in Contact entity.
 */
public enum ContactKind {
    INDIVIDUAL("individual"),
    BUSINESS("business");

    private final String value;

    ContactKind(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ContactKind fromValue(String value) {
        for (ContactKind kind : ContactKind.values()) {
            if (kind.value.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown contact kind value: " + value);
    }
}

