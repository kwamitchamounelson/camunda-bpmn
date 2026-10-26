package com.formation.pret.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AttribuerTachePretRequest", description = "Prise en charge d'une tache de pret")
public record AttribuerTachePretRequest(

        @Schema(description = """
                Utilisateur qui prend la tache : conseiller, expert, analyste, comite, notaire
                (un par groupe candidat), ou demo.""",
                example = "analyste", requiredMode = Schema.RequiredMode.REQUIRED)
        String utilisateur) {
}
