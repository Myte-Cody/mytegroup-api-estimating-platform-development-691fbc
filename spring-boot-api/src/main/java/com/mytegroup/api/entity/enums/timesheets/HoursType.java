package com.mytegroup.api.entity.enums.timesheets;

/**
 * Timesheet hours type enum.
 */
public enum HoursType {
    REGULAR("regular"),
    OVERTIME("overtime"),
    DOUBLETIME("doubletime");

    private final String value;

    HoursType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static HoursType fromValue(String value) {
        for (HoursType type : HoursType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown hours type value: " + value);
    }
}
