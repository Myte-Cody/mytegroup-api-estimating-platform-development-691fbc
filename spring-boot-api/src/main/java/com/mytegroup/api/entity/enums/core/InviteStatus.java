package com.mytegroup.api.entity.enums.core;

/**
 * Invite status enum.
 * Used in Invite entity.
 */
public enum InviteStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    EXPIRED("expired");

    private final String value;

    InviteStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static InviteStatus fromValue(String value) {
        for (InviteStatus status : InviteStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown invite status value: " + value);
    }
}

