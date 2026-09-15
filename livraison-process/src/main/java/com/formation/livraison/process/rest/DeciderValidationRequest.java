package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DeciderValidationRequest",
        description = "Decision du validateur sur Task_Validation, equivalent du formulaire de la Tasklist")
public record DeciderValidationRequest(

        @Schema(description = "true : la commande part en preparation. false : elle est annulee.",
                example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean approuve,

        @Schema(description = "Motif du refus, ignore si approuve vaut true. "
                + "Vide en cas de refus, le motif enregistre sera 'Non precise'.",
                example = "Montant trop eleve pour ce client", nullable = true)
        String motifRefus) {
}
