package com.mytegroup.api.entity.enums.projects;

/**
 * Seat status enum.
 * Used in Seat entity.
 */
public enum SeatStatus {
    VACANT("vacant"),
    ACTIVE("active");

    private final String value;

    SeatStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SeatStatus fromValue(String value) {
        for (SeatStatus status : SeatStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown seat status value: " + value);
    }
}

