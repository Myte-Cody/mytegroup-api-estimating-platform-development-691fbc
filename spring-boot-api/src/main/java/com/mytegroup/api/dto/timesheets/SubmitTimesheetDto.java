package com.mytegroup.api.dto.timesheets;

public record SubmitTimesheetDto(
    String submittedBy
) {
    public SubmitTimesheetDto {
        if (submittedBy != null) {
            submittedBy = submittedBy.trim();
        }
    }
}
