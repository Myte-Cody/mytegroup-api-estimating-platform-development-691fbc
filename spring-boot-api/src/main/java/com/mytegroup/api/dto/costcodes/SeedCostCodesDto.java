package com.mytegroup.api.dto.costcodes;

public record SeedCostCodesDto(
    String pack,
    Boolean replace
) {
    public SeedCostCodesDto {
        if (pack != null) {
            pack = pack.trim();
        }
    }
}

