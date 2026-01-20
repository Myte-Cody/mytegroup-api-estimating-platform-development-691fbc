package com.mytegroup.api.entity.enums.projects;

/**
 * Estimate status enum.
 * Used in Estimate entity.
 */
public enum EstimateStatus {
    DRAFT("draft"),
    FINAL("final"),
    ARCHIVED("archived");

    private final String value;

    EstimateStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EstimateStatus fromValue(String value) {
        for (EstimateStatus status : EstimateStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown estimate status value: " + value);
    }
}

