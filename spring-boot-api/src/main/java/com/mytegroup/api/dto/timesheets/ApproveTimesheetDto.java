package com.mytegroup.api.dto.timesheets;

public record ApproveTimesheetDto(
    String approverId,
    String comments
) {
    public ApproveTimesheetDto {
        if (approverId != null) {
            approverId = approverId.trim();
        }
        if (comments != null) {
            comments = comments.trim();
        }
    }
}
