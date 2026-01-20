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
public class EstimateLineItem {

    @Column(name = "code")
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "total")
    private Double total;
}

