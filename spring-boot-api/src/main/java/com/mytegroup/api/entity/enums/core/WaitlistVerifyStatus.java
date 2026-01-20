package com.mytegroup.api.entity.enums.core;

/**
 * Waitlist verification status enum.
 * Used in WaitlistEntry entity (verifyStatus, phoneVerifyStatus fields).
 */
public enum WaitlistVerifyStatus {
    UNVERIFIED("unverified"),
    VERIFIED("verified"),
    BLOCKED("blocked");

    private final String value;

    WaitlistVerifyStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WaitlistVerifyStatus fromValue(String value) {
        for (WaitlistVerifyStatus status : WaitlistVerifyStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown waitlist verify status value: " + value);
    }
}

