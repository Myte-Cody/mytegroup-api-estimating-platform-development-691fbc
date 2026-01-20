package com.mytegroup.api.entity.cost.embeddable;

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
public class CostCodeImportPreview {

    @Column(name = "category")
    private String category;

    @Column(name = "code")
    private String code;

    @Column(name = "description")
    private String description;
}

