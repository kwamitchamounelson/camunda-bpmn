package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MouvementStockRequest", description = "Entree ou sortie de stock, relative au stock courant")
public record MouvementStockRequest(

        @Schema(description = "Quantite signee : positive pour une entree, negative pour une sortie. "
                + "Une sortie qui rendrait le stock negatif est refusee et ne modifie rien.",
                example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int mouvement) {
}
