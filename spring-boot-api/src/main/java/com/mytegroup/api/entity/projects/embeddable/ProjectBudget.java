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
public class ProjectBudget {

    @Column(name = "hours")
    private Double hours;

    @Column(name = "labour_rate")
    private Double labourRate;

    @Column(name = "currency")
    private String currency;

    @Column(name = "amount")
    private Double amount;
}

