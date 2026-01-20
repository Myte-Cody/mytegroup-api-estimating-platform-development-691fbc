package com.mytegroup.api.entity.enums.cost;

/**
 * Cost code import status enum.
 * Used in CostCodeImportJob entity.
 */
public enum CostCodeImportStatus {
    QUEUED("queued"),
    PROCESSING("processing"),
    PREVIEW("preview"),
    DONE("done"),
    FAILED("failed");

    private final String value;

    CostCodeImportStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CostCodeImportStatus fromValue(String value) {
        for (CostCodeImportStatus status : CostCodeImportStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown cost code import status value: " + value);
    }
}

