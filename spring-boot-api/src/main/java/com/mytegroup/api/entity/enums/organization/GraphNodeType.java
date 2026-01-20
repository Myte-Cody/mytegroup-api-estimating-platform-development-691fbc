package com.mytegroup.api.entity.enums.organization;

/**
 * Graph node type enum.
 * Used in GraphEdge entity (fromNodeType, toNodeType fields).
 */
public enum GraphNodeType {
    PERSON("person"),
    ORG_LOCATION("org_location"),
    COMPANY("company"),
    COMPANY_LOCATION("company_location");

    private final String value;

    GraphNodeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GraphNodeType fromValue(String value) {
        for (GraphNodeType type : GraphNodeType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown graph node type value: " + value);
    }
}

