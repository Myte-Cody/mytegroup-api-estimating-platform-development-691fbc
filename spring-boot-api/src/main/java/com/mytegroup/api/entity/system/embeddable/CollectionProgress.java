package com.mytegroup.api.entity.system.embeddable;

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
public class CollectionProgress {

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "copied", nullable = false)
    private Integer copied;

    @Column(name = "last_id")
    private String lastId;
}

