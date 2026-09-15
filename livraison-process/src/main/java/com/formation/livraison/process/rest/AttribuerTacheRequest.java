package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AttribuerTacheRequest", description = "Prise en charge d'une tache par un utilisateur")
public record AttribuerTacheRequest(

        @Schema(description = """
                Utilisateur qui prend la tache. Comptes crees au demarrage :
                employeLogistique (groupe logistique), manager (managers), directeur (directeurs),
                et demo (admin Camunda).""",
                example = "employeLogistique", requiredMode = Schema.RequiredMode.REQUIRED)
        String utilisateur) {
}
