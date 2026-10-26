package com.formation.pret.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "CompleterTachePretRequest", description = """
        Variables attendues selon la tache :
        - Task_Completer   : revenusMensuels, valeurBien, commentaireCompletude
        - Task_Expertise   : valeurExpertisee, commentaireExpertise
        - Task_Analyse / Task_Comite : pretAccorde, tauxPropose, motifRefus
        - Task_Acceptation : offreAcceptee
        - Task_Signature   : dateSignature""")
public record CompleterTachePretRequest(

        @Schema(example = "{\"pretAccorde\": true, \"tauxPropose\": \"3.2\"}", nullable = true)
        Map<String, Object> variables) {
}
