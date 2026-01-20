package com.mytegroup.api.entity.projects.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CostCodeBudget {

    @Column(name = "cost_code_id", nullable = false)
    private Long costCodeId;

    @Column(name = "budgeted_hours")
    private Double budgetedHours;

    @Column(name = "cost_budget")
    private Double costBudget;
}

