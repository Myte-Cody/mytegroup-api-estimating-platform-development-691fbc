package com.mytegroup.api.dto.projects;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CreateProjectDto(
    String name,
    String description,
    String orgId,
    String officeId,
    String projectCode,
    String status,
    String location,
    LocalDate bidDate,
    LocalDate awardDate,
    LocalDate fabricationStartDate,
    LocalDate fabricationEndDate,
    LocalDate erectionStartDate,
    LocalDate erectionEndDate,
    LocalDate completionDate,
    Map<String, Object> budget,
    Map<String, Object> quantities,
    Map<String, Object> staffing,
    List<Map<String, Object>> costCodeBudgets
) {
    public CreateProjectDto {
        if (name != null) {
            name = name.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (projectCode != null) {
            projectCode = projectCode.trim();
        }
        if (status != null) {
            status = status.trim();
        }
        if (location != null) {
            location = location.trim();
        }
    }
}



