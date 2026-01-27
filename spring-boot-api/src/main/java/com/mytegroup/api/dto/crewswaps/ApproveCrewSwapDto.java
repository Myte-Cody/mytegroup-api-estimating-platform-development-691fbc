package com.mytegroup.api.dto.crewswaps;

public record ApproveCrewSwapDto(
    String approverId,
    String comments
) {
    public ApproveCrewSwapDto {
        if (approverId != null) {
            approverId = approverId.trim();
        }
        if (comments != null) {
            comments = comments.trim();
        }
    }
}
