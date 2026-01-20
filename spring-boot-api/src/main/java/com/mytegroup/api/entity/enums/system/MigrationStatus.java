package com.mytegroup.api.entity.enums.system;

/**
 * Migration status enum.
 * Used in TenantMigration entity.
 */
public enum MigrationStatus {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    READY_FOR_CUTOVER("ready_for_cutover"),
    COMPLETED("completed"),
    FAILED("failed"),
    ABORTED("aborted");

    private final String value;

    MigrationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MigrationStatus fromValue(String value) {
        for (MigrationStatus status : MigrationStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown migration status value: " + value);
    }
}

