package com.mytegroup.api.dto.crewswaps;

public record CancelCrewSwapDto(
    String canceledBy,
    String reason
) {
    public CancelCrewSwapDto {
        if (canceledBy != null) {
            canceledBy = canceledBy.trim();
        }
        if (reason != null) {
            reason = reason.trim();
        }
    }
}
