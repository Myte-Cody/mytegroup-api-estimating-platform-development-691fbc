package com.mytegroup.api.entity.enums.system;

/**
 * Migration direction enum.
 * Used in TenantMigration entity.
 */
public enum MigrationDirection {
    SHARED_TO_DEDICATED("shared_to_dedicated"),
    DEDICATED_TO_SHARED("dedicated_to_shared");

    private final String value;

    MigrationDirection(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MigrationDirection fromValue(String value) {
        for (MigrationDirection direction : MigrationDirection.values()) {
            if (direction.value.equals(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Unknown migration direction value: " + value);
    }
}

