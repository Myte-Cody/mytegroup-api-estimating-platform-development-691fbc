package com.mytegroup.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetEntryResponseDto {
    private Long id;
    private String taskId;
    private Double hours;
    private String hoursType;
    private String notes;
}
