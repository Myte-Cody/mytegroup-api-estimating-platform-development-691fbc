package com.mytegroup.api.entity.enums.organization;

/**
 * Data residency enum.
 * Used in Organization entity.
 */
public enum DataResidency {
    SHARED("shared"),
    DEDICATED("dedicated");

    private final String value;

    DataResidency(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DataResidency fromValue(String value) {
        for (DataResidency residency : DataResidency.values()) {
            if (residency.value.equals(value)) {
                return residency;
            }
        }
        throw new IllegalArgumentException("Unknown data residency value: " + value);
    }
}

