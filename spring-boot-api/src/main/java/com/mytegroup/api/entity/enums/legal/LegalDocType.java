package com.mytegroup.api.entity.enums.legal;

/**
 * Legal document type enum.
 * Used in LegalDoc, LegalAcceptance entities.
 */
public enum LegalDocType {
    PRIVACY_POLICY("privacy_policy"),
    TERMS("terms");

    private final String value;

    LegalDocType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LegalDocType fromValue(String value) {
        for (LegalDocType type : LegalDocType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown legal doc type value: " + value);
    }
}

