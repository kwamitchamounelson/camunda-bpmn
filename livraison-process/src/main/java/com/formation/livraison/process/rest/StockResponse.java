package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StockResponse", description = "Etat du stock d'un produit")
public record StockResponse(

        @Schema(example = "clavier")
        String produit,

        @Schema(description = "Quantite disponible", example = "25")
        int quantite,

        @Schema(description = "Tarif applique par PricingService ; 100.0 pour un produit hors catalogue",
                example = "45.0")
        double prixUnitaire) {
}
