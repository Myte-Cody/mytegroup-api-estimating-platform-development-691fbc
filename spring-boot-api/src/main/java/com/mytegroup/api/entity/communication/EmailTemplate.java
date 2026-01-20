package com.mytegroup.api.entity.communication;

import com.mytegroup.api.entity.BaseEntity;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "email_templates", indexes = {
    @Index(name = "idx_email_template_org_name_locale", columnList = "org_id, name, locale", unique = true)
})
@Audited
@Getter
@Setter
public class EmailTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "locale", nullable = false)
    private String locale = "en"; // EMAIL_TEMPLATE_DEFAULT_LOCALE

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "html", nullable = false, columnDefinition = "TEXT")
    private String html;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @ElementCollection
    @CollectionTable(name = "email_template_required_variables", joinColumns = @JoinColumn(name = "email_template_id"))
    @Column(name = "variable")
    private List<String> requiredVariables = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "email_template_optional_variables", joinColumns = @JoinColumn(name = "email_template_id"))
    @Column(name = "variable")
    private List<String> optionalVariables = new ArrayList<>();

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedByUser;

    @Column(name = "pii_stripped", nullable = false)
    private Boolean piiStripped = false;

    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = false;
}

