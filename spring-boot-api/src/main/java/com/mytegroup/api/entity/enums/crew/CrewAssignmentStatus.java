package com.mytegroup.api.entity.enums.crew;

/**
 * Crew assignment status enum.
 */
public enum CrewAssignmentStatus {
    ACTIVE("active"),
    COMPLETED("completed"),
    CANCELED("canceled"),
    ARCHIVED("archived");

    private final String value;

    CrewAssignmentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CrewAssignmentStatus fromValue(String value) {
        for (CrewAssignmentStatus status : CrewAssignmentStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown crew assignment status value: " + value);
    }
}
