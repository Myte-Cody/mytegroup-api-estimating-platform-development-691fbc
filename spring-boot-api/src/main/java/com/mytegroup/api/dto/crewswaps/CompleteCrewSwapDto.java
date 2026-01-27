package com.mytegroup.api.dto.crewswaps;

public record CompleteCrewSwapDto(
    String completedBy,
    String comments
) {
    public CompleteCrewSwapDto {
        if (completedBy != null) {
            completedBy = completedBy.trim();
        }
        if (comments != null) {
            comments = comments.trim();
        }
    }
}
