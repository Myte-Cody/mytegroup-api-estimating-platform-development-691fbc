package com.mytegroup.api.common.enums;

/**
 * Role enum shared across multiple domains.
 * Used in User, Invite, and other entities.
 */
public enum Role {
    SUPER_ADMIN("superadmin"),
    PLATFORM_ADMIN("platform_admin"),
    ORG_OWNER("org_owner"),
    ORG_ADMIN("org_admin"),
    MANAGER("manager"),
    VIEWER("viewer"),
    ADMIN("admin"),
    COMPLIANCE_OFFICER("compliance_officer"),
    SECURITY_OFFICER("security_officer"),
    PM("pm"),
    ESTIMATOR("estimator"),
    ENGINEER("engineer"),
    DETAILER("detailer"),
    TRANSPORTER("transporter"),
    FOREMAN("foreman"),
    SUPERINTENDENT("superintendent"),
    QAQC("qaqc"),
    HS("hs"),
    PURCHASING("purchasing"),
    COMPLIANCE("compliance"),
    SECURITY("security"),
    FINANCE("finance"),
    USER("user");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Role fromValue(String value) {
        for (Role role : Role.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role value: " + value);
    }
}

