package com.mytegroup.api.entity.enums.communication;

/**
 * Contact inquiry status enum.
 * Used in ContactInquiry entity.
 */
public enum ContactInquiryStatus {
    NEW("new"),
    IN_PROGRESS("in-progress"),
    CLOSED("closed");

    private final String value;

    ContactInquiryStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ContactInquiryStatus fromValue(String value) {
        for (ContactInquiryStatus status : ContactInquiryStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown contact inquiry status value: " + value);
    }
}

