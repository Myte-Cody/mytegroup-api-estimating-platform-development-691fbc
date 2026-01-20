package com.mytegroup.api.entity.people.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonEmail {

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "normalized", nullable = false)
    private String normalized;

    @Column(name = "label")
    private String label;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}

