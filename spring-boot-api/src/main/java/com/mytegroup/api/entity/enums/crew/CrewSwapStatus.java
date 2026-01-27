package com.mytegroup.api.entity.enums.crew;

/**
 * Crew swap status enum.
 */
public enum CrewSwapStatus {
    REQUESTED("requested"),
    APPROVED("approved"),
    REJECTED("rejected"),
    COMPLETED("completed"),
    CANCELED("canceled"),
    ARCHIVED("archived");

    private final String value;

    CrewSwapStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CrewSwapStatus fromValue(String value) {
        for (CrewSwapStatus status : CrewSwapStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown crew swap status value: " + value);
    }
}
