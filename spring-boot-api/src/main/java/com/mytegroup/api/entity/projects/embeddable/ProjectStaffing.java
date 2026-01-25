package com.mytegroup.api.entity.projects.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStaffing {

    @Column(name = "project_manager_person_id")
    private Long projectManagerPersonId;

    @Column(name = "foreman_person_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String foremanPersonIds; // JSON array of Long IDs

    @Column(name = "superintendent_person_id")
    private Long superintendentPersonId;
}

