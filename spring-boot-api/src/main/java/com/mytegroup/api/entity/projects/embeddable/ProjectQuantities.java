package com.mytegroup.api.entity.projects.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectQuantities {

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuralQuantity {
        @Column(name = "tonnage")
        private Double tonnage;

        @Column(name = "pieces")
        private Integer pieces;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiscMetalsQuantity {
        @Column(name = "tonnage")
        private Double tonnage;

        @Column(name = "pieces")
        private Integer pieces;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetalDeckQuantity {
        @Column(name = "pieces")
        private Integer pieces;

        @Column(name = "sqft")
        private Double sqft;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CltPanelsQuantity {
        @Column(name = "pieces")
        private Integer pieces;

        @Column(name = "sqft")
        private Double sqft;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlulamQuantity {
        @Column(name = "volume_m3")
        private Double volumeM3;

        @Column(name = "pieces")
        private Integer pieces;
    }

    // Note: These will be stored as JSONB in PostgreSQL
    // Using @JdbcTypeCode(SqlTypes.JSON) to properly handle JSONB type conversion
    @Column(name = "structural", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String structural; // JSON representation

    @Column(name = "misc_metals", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String miscMetals; // JSON representation

    @Column(name = "metal_deck", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metalDeck; // JSON representation

    @Column(name = "clt_panels", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String cltPanels; // JSON representation

    @Column(name = "glulam", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String glulam; // JSON representation
}

