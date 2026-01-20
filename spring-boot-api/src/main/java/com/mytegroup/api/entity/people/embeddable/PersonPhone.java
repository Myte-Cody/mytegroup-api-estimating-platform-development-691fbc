package com.mytegroup.api.entity.people.embeddable;

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
public class PersonPhone {

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "e164", nullable = false)
    private String e164;

    @Column(name = "label")
    private String label;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;
}

