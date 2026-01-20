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
    // For JPA, we'll use @Type(JsonBinaryType.class) or store as JSONB column
    // For now, we'll use separate columns or JSONB
    @Column(name = "structural", columnDefinition = "jsonb")
    private String structural; // JSON representation

    @Column(name = "misc_metals", columnDefinition = "jsonb")
    private String miscMetals; // JSON representation

    @Column(name = "metal_deck", columnDefinition = "jsonb")
    private String metalDeck; // JSON representation

    @Column(name = "clt_panels", columnDefinition = "jsonb")
    private String cltPanels; // JSON representation

    @Column(name = "glulam", columnDefinition = "jsonb")
    private String glulam; // JSON representation
}

