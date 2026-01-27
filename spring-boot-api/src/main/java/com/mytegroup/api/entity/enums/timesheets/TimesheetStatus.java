package com.mytegroup.api.entity.enums.timesheets;

/**
 * Timesheet status enum.
 */
public enum TimesheetStatus {
    DRAFT("draft"),
    SUBMITTED("submitted"),
    APPROVED("approved"),
    REJECTED("rejected"),
    ARCHIVED("archived");

    private final String value;

    TimesheetStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TimesheetStatus fromValue(String value) {
        for (TimesheetStatus status : TimesheetStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown timesheet status value: " + value);
    }
}
