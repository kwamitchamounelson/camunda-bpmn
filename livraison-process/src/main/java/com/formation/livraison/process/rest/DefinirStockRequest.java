package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DefinirStockRequest", description = "Nouvelle quantite absolue pour un produit")
public record DefinirStockRequest(

        @Schema(description = "Quantite disponible apres l'operation. Zero met le produit en rupture.",
                example = "10", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int quantite) {
}
