package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErreurResponse", description = "Erreur fonctionnelle renvoyee par l'API")
public record ErreurResponse(

        @Schema(example = "Commande introuvable : CMD-INCONNUE")
        String message) {
}
