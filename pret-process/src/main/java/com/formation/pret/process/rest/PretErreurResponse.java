package com.formation.pret.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PretErreurResponse", description = "Erreur fonctionnelle renvoyee par l'API pret")
public record PretErreurResponse(

        @Schema(example = "Demande de pret introuvable : PRET-INCONNUE")
        String message) {
}
