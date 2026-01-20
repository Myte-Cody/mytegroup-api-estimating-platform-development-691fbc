package com.mytegroup.api.entity.enums.core;

/**
 * Waitlist status enum.
 * Used in WaitlistEntry entity.
 */
public enum WaitlistStatus {
    PENDING_COHORT("pending-cohort"),
    INVITED("invited"),
    ACTIVATED("activated");

    private final String value;

    WaitlistStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WaitlistStatus fromValue(String value) {
        for (WaitlistStatus status : WaitlistStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown waitlist status value: " + value);
    }
}

